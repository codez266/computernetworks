import sys
from enum import Enum
# Enum for Fixed or Bursty source


class Type(Enum):
    FIXED = 1
    BURSTY = 2


class PktError(Exception):
    def __init__(self, msg):
        self.msg = msg


class Device:
    """
    Device object representing any packet sending/recieving device in general
    """
    def __init__(self, id):
        """
        Initialize device with an id

        :param id: unique id of the device object
        """
        self.id = id
        self.timeToTransmit = 0
        self.delay = 0
        self.dropped = 0

    def getTimeToTransmit(self):
        return self.timeToTransmit

    def setTimeToTransmit(self, time):
        self.timeToTransmit = time


class Source(Device):
    """
    Source object having an id, type, rate.
    """
    lim = sys.maxint
    pktlimit = 100# hardcoded

    def __init__(self, id, rate, bw, pktsize):
        """
        Initializer for a source object.

        :param id: unique id of the source object
        :type id: int
        :param type: Type of the source, fixed or bursty
        :type type: @Class Type
        :param rate: Rate at which packets are emitted
        :param bw: bandwidth of transfer
        """
        Device.__init__(self, id)
        self.rate = rate
        self.queue = []
        self.bw = bw
        self.pktsize = pktsize
        self.totalpkts = 0

    def generatePacket(self, time):
        packet = Packet(time, self.id,  self.pktsize)
        if len(self.queue) < self.lim:
            Event.getSim().addPacket(packet)
            self.queue.append(packet.id)
            self.totalpkts = self.totalpkts + 1
            self.schedulePacket(time + 1.0 / self.rate)
            self.delay = self.delay - time
            return packet
        else:
            self.dropped = self.dropped + 1
            raise PktError('Packet Dropped')

    def sendPacket(self, time):
        self.delay = self.delay + time
        pkt = self.queue.pop()
        return pkt

    def isPacketInSource(self, pid):
        if pid in self.queue:
            return True
    
    def schedulePacket(self, time):
        #print 'total pkts %d'%(self.totalpkts)
        if self.totalpkts <= Source.pktlimit:
            Event.addEvent(self.id, time)
    
    def getAvgQueueDelay( self ):
        #print self.delay
        return 1.0 * self.delay / self.totalpkts

class FixedSource(Source):
    """
    Fixed source object inheriting from Source, representing a fixed source.
    """
    def __init__(self, id, rate, bw, pktsize):
        Source.__init__(self, id, rate, bw, pktsize) 


class BurstySource(Source):
    def __init__(self, id, rate, bw, pktsize, bsize):
        Source.__init__(self, id, rate, bw, pktsize)
        self.bsize = bsize
        self.pktCountInBurst = 0
           
    def generatePacket(self, time):
        packet = Packet(time, self.id,  self.pktsize)
        if len(self.queue) < self.lim:
            Event.getSim().addPacket(packet)
            self.queue.append(packet.id)
            self.totalpkts = self.totalpkts + 1
            # keep generating packets at this instant till burst fullfilled
            if self.pktCountInBurst < self.bsize:
                self.pktCountInBurst = self.pktCountInBurst + 1
                self.schedulePacket(time)
            else:
                self.schedulePacket(time + 1.0 / self.rate)
                self.pktCountInBurst = 0
            return packet
        else:
            self.dropped = self.dropped + 1
            raise PktError('Packet Dropped')

class Switch(Device):
    lim = sys.maxint

    def __init__(self, id, incoming, bw, pktsize):
        Device.__init__(self, id)
        self.inc = incoming
        self.bw = bw
        self.queue = []
        self.pktsize = pktsize
        self.timeToTransmit = -1
        self.delay = 0
        self.totalpkts = 0

    def receivePacket(self, pktid, time):
        pass

    def sendPacket(self):
        pass

    def getAvgQueueDelay(self):
        #print "%d,%d"%(self.delay,self.totalpkts)
        return 1.0 * self.delay / self.totalpkts


class CSwitch(Switch):
    def __init__(self, id, incoming, bw, pktsize):
        Switch.__init__(self, id, incoming, bw, pktsize)
        self.curr = 0
        # Generate queues for sources
        for x in range(incoming):
            self.queue.append([])

    def receivePacket(self, pktid, time):
        pkt = Event.getSim().getPacket(pktid)
        current = pkt.fromId
        currQueue = self.queue[current]
        if len(currQueue) > Switch.lim:
            self.dropped = self.dropped + 1
            raise PktError('Packet Dropped at Switch, queue #%d' % (curr))
        else:
            self.totalpkts = self.totalpkts + 1
            currQueue.append(pktid)

    def sendPacket(self, time):
        # if there's a packet to send
        if len(self.queue[curr]) > 0:
            pktid = self.queue[curr].pop()
            # put packet back in event processing
            # use api here!
            Event.queue.append(Event(self.curr, pktid, time + self.pktsize / self.bw, 5))
        self.scheduleDispatch(time + self.pktsize/self.bw)
        self.curr = (self.curr + 1) % self.incoming
        return pktid

    def scheduleDispatch(self,time):
        Event.queue.append(Event(self.curr, -1, time, 9))


class PSwitch(Switch):

    def __init__(self, id, incoming, bw, pktsize):
        Switch.__init__(self, id, incoming, bw, pktsize)
        # Single queue for all packets

    def receivePacket(self, pktid, time):
        if len(self.queue) > Switch.lim:
            self.dropped = self.dropped + 1
            raise PktError('Packet Dropped at Switch')
        else:
            #print 'pushing %d'%(pktid)
            self.totalpkts = self.totalpkts + 1
            self.delay = self.delay - time
            self.queue.append(pktid)

    def sendPacket(self, time):
        self.delay = self.delay + time
        pktid = self.queue.pop()
        #print 'popping %d'%(pktid)
        return pktid


class Packet:
    id = 0

    def __init__(self, timestamp, fromId, size):
        self.id = Packet.id
        Packet.id = Packet.id + 1
        self.timestamp = timestamp
        self.fromId = fromId
        self.size = size


class Event:
    """
    We have an event object that represents the list of events to occur in
    order.

    Four types of events have been defined -
    1. Generate a packet
    2. Send a packet from Source
    4. Recieve a packet on the Switch
    5. Send a packet from the Switch
    7. Recieve a packet on sink
    8. Drop a packet
    9. TDM alarm
    """
    queue = []

    def __init__(self, sid, pid, timestamp, state):
        self.sid = sid
        self.pid = pid
        self.timestamp = timestamp
        self.state = state

    def __cmp__(self, other):
        """Total ordering in events based on timestamp"""
        if (self.timestamp == other.timestamp):
            return self.state.__cmp__(other.state)
        else:
            return self.timestamp < other.timestamp
            #return self.timestamp.__cmp__(other.timestamp)

    def getKey(self, event):
        return event.timestamp

    def processEvent(self):
        if self.state == 1:
            # process generation
            try:
                pkt = Event.getSim().getSource(self.sid).generatePacket(self.timestamp)
                #print 'generating at %d'%(self.timestamp)
                self.pid = pkt.id
                source = Event.getSim().getSource(self.sid)
                # for next packet generation
                #print 'generated at %d, scheduling next at %d' % (self.timestamp, self.timestamp+source.rate)
                self.state = 2
                if source.getTimeToTransmit() > self.timestamp:
                    # a valid condition for queueing
                    # queue packet for a later time depending on when it can get a
                    # slot
                    self.timestamp = source.getTimeToTransmit()
                    source.setTimeToTransmit(source.getTimeToTransmit() + 1.0 * source.pktsize / source.bw)
                else:
                    source.setTimeToTransmit(self.timestamp + 1.0 * source.pktsize / source.bw)
            except PktError as e:
                #print e.msg
                # send this event to die state
                self.state = 7
        elif self.state == 2:
            source = Event.getSim().getSource(self.sid)
            source.sendPacket(self.timestamp)# remove pkt from source
            self.state = 4
            self.timestamp = self.timestamp + 1.0 * source.pktsize / source.bw
        elif self.state == 4:
            pkt = Event.getSim().getPacket(self.pid)
            # using a hardcoded switch index with 0 id
            switch = Event.getSim().getSwitch(0)
            try:
                switch.receivePacket(self.pid, self.timestamp)
                self.state = 5
                if self.timestamp < switch.getTimeToTransmit():
                    # only for packet switching, for tdm, this will change
                    if isinstance(switch, PSwitch):
                        self.timestamp = switch.getTimeToTransmit()
                        switch.setTimeToTransmit(switch.getTimeToTransmit() +
                                                 1.0 * Event.getSim().getPacket(self.pid).size / switch.bw)
                    else:
                        # for tdm, remove this event, and rely on the alarm at
                        # regular intervals
                        del Event.queue[0] 
                else:
                    switch.setTimeToTransmit(self.timestamp + 1.0 * Event.getSim().getPacket(self.pid).size / switch.bw)
            except PktError as e:
                #print e.msg
                # send event to die state
                self.state = 8
        elif self.state == 5:
            switch = Event.getSim().getSwitch(0)
            #print 'packet from source %d at time %d' % (self.sid,self.timestamp)
            switch.sendPacket(self.timestamp)#remove packet from switch
            self.state = 7
            self.timestamp = self.timestamp + Event.getSim().getPacket(self.pid).size / switch.bw
        elif self.state == 7:
            try:
                sink = Event.getSim().getSwitch(1)# hardcoded 1
                sink.receivePacket(self.pid, self.timestamp)
                self.state = 8
            except PktError as e:
                #print e.msg
                self.state = 8
        elif self.state == 8:
            #print "destroying packet ", self.pid
            del Event.queue[0]# remove head event
        elif self.state == 9:
            switch = Event.getSim().getSwitch(0)
            switch.sendPacket(self.timestamp)

        #Event.queue.sort()
        Event.queue = sorted(Event.queue, key=self.getKey)
        #st = ''
        #for i in range(len(Event.queue)):
        #    st = st + str(Event.queue[i].timestamp) + ','
        #    str(Event.queue[i].state) + ' '
        #print st

    @classmethod
    def initializeSim(cls, sim):
        cls.sim = sim

    @classmethod
    def getSim(cls):
        return cls.sim

    @classmethod
    def addEvent(cls, id, timestamp):
        """
        Adds an event to the event queue.
        Only supports adding state 1 events, i.e., initial
        :param id:source id associated with the event
        """
        Event.queue.append(Event(id, -1, timestamp, 1))
        Event.queue.sort()

    @classmethod
    def hasNextEvent(cls):
        return len(Event.queue) > 0

    @classmethod
    def nextEvent(cls):
        event = Event.queue[0]
        event.processEvent()


class Simulator:

    def __init__(self, bw, pktsize):
        # initialize the event class's Simulator
        Event.initializeSim(self)
        self.bandwidth = bw
        self.pktsize = pktsize
        self.sourcelist = []
        self.pktlist = {}
        self.switchlist = []

    def createSource(self, type, rate, bsize):
        id = len(self.sourcelist)
        if type == 1:
            self.sourcelist.append(FixedSource(id, rate, self.bandwidth, self.pktsize))
        else:
            self.sourcelist.append(BurstySource(id, rate, self.bandwidth, self.pktsize, bsize))
        return self.sourcelist[-1]

    def getSource(self, id):
        if id < len(self.sourcelist):
            return self.sourcelist[id]

    def createSwitch(self, incoming, type):
        id = len(self.switchlist)
        if type == 1:
            self.switchlist.append(CSwitch(id, incoming, self.bandwidth, self.pktsize))
        else:
            self.switchlist.append(PSwitch(id, incoming, self.bandwidth, self.pktsize))
        return self.switchlist[-1]

    def getSwitch(self, id):
        if id < len(self.switchlist):
            return self.switchlist[id]

    def addPacket(self, pkt):
        self.pktlist[pkt.id] = pkt

    def getPacket(self, id):
        if id in self.pktlist.keys():
            return self.pktlist[id]

    @classmethod
    def testSet1a(cls):
        #avg queue delay wrt queue size
        dataset = {}
        for i in range(10):
            queuesize = i + 5
            Source.lim = queuesize
            Switch.lim = 5
            # bandwidth, pktsize
            sim = Simulator(2, 18)
            # type, rate, burst size
            sim.createSource(1, 2, 3)
            sim.createSource(1, 3, 1)
            sim.createSwitch(1)
            sim.getSource(0).schedulePacket(2)
            sim.getSource(1).schedulePacket(3)
            # print sim.getSource(0).pktsize
            while Event.hasNextEvent():
                Event.nextEvent()
            dataset[queuesize] = sim.getSource(0).getAvgQueueDelay()
        for i in dataset:
            print str(i) + '\t' + str(dataset[i])

    @classmethod
    def testSet1b(cls):
        #avg queue delay wrt queue size
        dataset = {}
        for i in range(10):
            psr = 10 + i
            Source.lim = 10000
            Switch.lim = 10000
            Packet.id = 0
            Source.pktlimit = 100
            # bandwidth, pktsize
            sim = Simulator(2, 9)
            # type, rate, burst size
            sim.createSource(1, psr, 3)
            sim.createSource(1, psr, 1)
            # incoming, type
            sim.createSwitch(2,2)
            sim.createSwitch(1,2)
            sim.getSource(0).schedulePacket(2)
            sim.getSource(1).schedulePacket(3)
            while Event.hasNextEvent():
                Event.nextEvent()
            dataset[psr] = sim.getSwitch(0).getAvgQueueDelay()
        for i in dataset:
            print str(i) + '\t' + str(dataset[i])

    @classmethod
    def testSet1c(cls):
        #avg queue delay wrt burst size
        dataset = {}
        for i in range(40):
            bs = i+1
            Source.lim = 10000
            Switch.lim = 10000
            Packet.id = 0
            Source.pktlimit = 200
            # bandwidth, pktsize
            sim = Simulator(2, 0.1)
            # type, rate, burst size
            sim.createSource(2, 3, bs)
            sim.createSource(2, 3, bs)
            # incoming, type
            sim.createSwitch(2,2)
            sim.createSwitch(1,2)
            sim.getSource(0).schedulePacket(2)
            sim.getSource(1).schedulePacket(3)
            while Event.hasNextEvent():
                Event.nextEvent()
            dataset[bs] = sim.getSwitch(0).getAvgQueueDelay()
        for i in dataset:
            print str(i) + '\t' + str(dataset[i])

    @classmethod
    def testSet1d(cls):
        #avg queue delay wrt burst interval
        dataset = {}
        for i in range(40):
            bintval = i+1
            Source.lim = 10000
            Switch.lim = 10000
            Packet.id = 0
            Source.pktlimit = 200
            # bandwidth, pktsize
            sim = Simulator(2, 0.05)
            # type, rate, burst size
            sim.createSource(2, bintval, 3)
            sim.createSource(2, bintval, 3)
            # incoming, type
            sim.createSwitch(2,2)
            sim.createSwitch(1,2)
            sim.getSource(0).schedulePacket(2)
            sim.getSource(1).schedulePacket(3)
            while Event.hasNextEvent():
                Event.nextEvent()
            dataset[i+1] = sim.getSwitch(0).getAvgQueueDelay()
        for i in dataset:
            print str(i) + '\t' + str(dataset[i])

    @classmethod
    def testSet2aSrc(cls):
        #avg queue delay wrt burst interval
        dataset = {}
        Source.lim = 10000
        Switch.lim = 10000
        Packet.id = 0
        Source.pktlimit = 200
        # bandwidth, pktsize
        sim = Simulator(2, 0.8)
        # type, rate, burst size
        sim.createSource(2, 2, 10)
        sim.createSource(2, 2, 10)
        # incoming, type
        sim.createSwitch(2,2)
        sim.createSwitch(1,2)
        sim.getSource(0).schedulePacket(2)
        sim.getSource(1).schedulePacket(3)
        while Event.hasNextEvent():
            event = Event.queue[0]
            time = event.timestamp
            dataset[time] = len(sim.getSwitch(0).queue)
            Event.nextEvent()
        keys = sorted(dataset)
        for i in keys:
            print str(i) + '\t' + str(dataset[i])

    @classmethod
    def testSet3Dropped(cls):
        #avg queue delay wrt burst interval
        dataset = {}
        for i in range(15):
            Source.lim = 100
            Switch.lim = 50 + i * 10
            Packet.id = 0
            Source.pktlimit = 200
            # bandwidth, pktsize
            sim = Simulator(2, 0.8)
            # type, rate, burst size
            sim.createSource(1, 0.2, 3)
            sim.createSource(1, 0.2, 3)
            # incoming, type
            sim.createSwitch(2,2)
            sim.createSwitch(1,2)
            sim.getSource(0).schedulePacket(2)
            sim.getSource(1).schedulePacket(3)
            while Event.hasNextEvent():
                Event.nextEvent()
            dataset[50+i*10] = sim.getSwitch(0).dropped
            
        keys = sorted(dataset)
        for i in keys:
            print str(i) + '\t' + str(dataset[i])

    @classmethod
    def testSet4(cls):
        #avg queue delay wrt burst interval
        dataset = {}
        for i in range(15):
            Source.lim = 200
            Switch.lim = 200
            Packet.id = 0
            Source.pktlimit = 200
            # bandwidth, pktsize
            sim = Simulator(2, 0.8)
            # type, rate, burst size
            sim.createSource(1, 1+0.2*i*10, 3)
            sim.createSource(1, 1+0.2*i*10, 3)
            # incoming, type
            sim.createSwitch(2,2)
            sim.createSwitch(1,2)
            sim.getSource(0).schedulePacket(2)
            sim.getSource(1).schedulePacket(3)
            timelast = 0
            while Event.hasNextEvent():
                timelast = Event.queue[0].timestamp
                Event.nextEvent()
            dataset[1+0.2*i*10] = sim.getSwitch(1).totalpkts / timelast
            
        keys = sorted(dataset)
        for i in keys:
            print str(i) + '\t' + str(dataset[i])

Simulator.testSet4()
