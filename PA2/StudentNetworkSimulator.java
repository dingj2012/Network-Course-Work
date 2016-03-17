import java.util.*;

public class StudentNetworkSimulator extends NetworkSimulator
{
    /*
     * Predefined Constants (static member variables):
     *
     *   int MAXDATASIZE : the maximum size of the Message data and
     *                     Packet payload
     *
     *   int A           : a predefined integer that represents entity A
     *   int B           : a predefined integer that represents entity B 
     *
     * Predefined Member Methods:
     *
     *  void stopTimer(int entity): 
     *       Stops the timer running at "entity" [A or B]
     *  void startTimer(int entity, double increment): 
     *       Starts a timer running at "entity" [A or B], which will expire in
     *       "increment" time units, causing the interrupt handler to be
     *       called.  You should only call this with A.
     *  void toLayer3(int callingEntity, Packet p)
     *       Puts the packet "p" into the network from "callingEntity" [A or B]
     *  void toLayer5(String dataSent)
     *       Passes "dataSent" up to layer 5
     *  double getTime()
     *       Returns the current time in the simulator.  Might be useful for
     *       debugging.
     *  int getTraceLevel()
     *       Returns TraceLevel
     *  void printEventList()
     *       Prints the current event list to stdout.  Might be useful for
     *       debugging, but probably not.
     *
     *
     *  Predefined Classes:
     *
     *  Message: Used to encapsulate a message coming from layer 5
     *    Constructor:
     *      Message(String inputData): 
     *          creates a new Message containing "inputData"
     *    Methods:
     *      boolean setData(String inputData):
     *          sets an existing Message's data to "inputData"
     *          returns true on success, false otherwise
     *      String getData():
     *          returns the data contained in the message
     *  Packet: Used to encapsulate a packet
     *    Constructors:
     *      Packet (Packet p):
     *          creates a new Packet that is a copy of "p"
     *      Packet (int seq, int ack, int check, String newPayload)
     *          creates a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and a
     *          payload of "newPayload"
     *      Packet (int seq, int ack, int check)
     *          create a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and
     *          an empty payload
     *    Methods:
     *      boolean setSeqnum(int n)
     *          sets the Packet's sequence field to "n"
     *          returns true on success, false otherwise
     *      boolean setAcknum(int n)
     *          sets the Packet's ack field to "n"
     *          returns true on success, false otherwise
     *      boolean setChecksum(int n)
     *          sets the Packet's checksum to "n"
     *          returns true on success, false otherwise
     *      boolean setPayload(String newPayload)
     *          sets the Packet's payload to "newPayload"
     *          returns true on success, false otherwise
     *      int getSeqnum()
     *          returns the contents of the Packet's sequence field
     *      int getAcknum()
     *          returns the contents of the Packet's ack field
     *      int getChecksum()
     *          returns the checksum of the Packet
     *      int getPayload()
     *          returns the Packet's payload
     *
     */
    
    /*   Please use the following variables in your routines.
     *   int WindowSize  : the window size
     *   double RxmtInterval   : the retransmission timeout
     *   int LimitSeqNo  : when sequence number reaches this value, it wraps around
     */
    
    public static final int FirstSeqNo = 0;
    private int WindowSize;
    private double RxmtInterval;
    private int LimitSeqNo;
    
    // Add any necessary class variables here.  Remember, you cannot use
    // these variables to send messages error free!  They can only hold
    // state information for A or B.
    
    private int numCorrupt;  // Number of corrupted packets
    private int numACK;      // Number of ACKs
    
    private int seqNum;      // The sequence number of current packet
    private int seqBase;     // begining of the window
    private int seqLast;     // Last of the window
    private int seqExpected; // The sequence number that B was expected to received
    
    private int nextPacket;              // next packet sent in window
    private int totalSentPacket;         // Number of packets sent
    private int totalDeliveredPacket;    // Number of packets successfully delivered to B

    private LinkedList<Double> Ttime;   // Linked List of transmitted time for sent packets
    private LinkedList<Packet> Psent;   // Linked List of sent packets
    
    // Also add any necessary methods (e.g. checksum of a String)
    
    //A TCP-like checksum, which consists of the sum of the (integer) sequence 
    //and ack field values, added to a character-by-character sum of the payload field of the packet
    protected int checkSum(Packet p)
    {   
        int checksum = p.getSeqnum() + p.getAcknum();
        String m = p.getPayload();
        for(int i = 0; i < m.length(); i++){
            checksum += (int) m.charAt(i);
        }
        
        return checksum;
    }
    
    //Increment the sequence number, if the incremented number exceeded window size, start over.
    protected int updateSeq(int num)
    {
        if (num == WindowSize){ 
            return FirstSeqNo; 
        }
        return num+1;
    } 
    
    //compute the average RTT for the final statistics
    protected double averageRTT()
    {
        double RTTmean = 0;int i;
        for(i = 0; i < seqBase; i++){
            RTTmean += Ttime.get(i);
        }
        
        return RTTmean = RTTmean / i;
    }
    
    // This is the constructor.  Don't touch!
    public StudentNetworkSimulator(int numMessages,
                                   double loss,
                                   double corrupt,
                                   double avgDelay,
                                   int trace,
                                   int seed,
                                   int winsize,
                                   double delay)
    {
        super(numMessages, loss, corrupt, avgDelay, trace, seed);
        WindowSize = winsize;
        LimitSeqNo = winsize+1;
        RxmtInterval = delay;
    }
    
    
    // This routine will be called whenever the upper layer at the sender [A]
    // has a message to send.  It is the job of your protocol to insure that
    // the data in such a message is delivered in-order, and correctly, to
    // the receiving upper layer.
    protected void aOutput(Message message)
    {
        Packet p = new Packet(seqNum, 0, -1, message.getData()); //create packet
        int check = checkSum(p); // calculate checksum of packet
        p.setChecksum(check); // set the checksum of packet
        seqNum = updateSeq(p.getSeqnum());  // update sequence number
        Psent.add(p); // add packet to the linked list
        
        if(Psent.indexOf(p) <= seqLast){
            toLayer3(A, p); // call of layer3; send to B
            
            //Every packet was stored in a linked list, a duplicated packet can be detected through the list 
            Ttime.add(Psent.indexOf(p), getTime()); 
            totalSentPacket++; 
            
            if(seqBase == nextPacket){ 
                startTimer(A, RxmtInterval);  //timer
            }
            nextPacket++; 
        }
    }
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by a B-side procedure)
    // arrives at the A-side.  "packet" is the (possibly corrupted) packet
    // sent from the B-side.
    protected void aInput(Packet packet)
    {
        
        //record receiving time
        double time = getTime();
        
        // If ACK is corrupted, timeout
        if (checkSum(packet) != packet.getChecksum()){
            System.out.println("A:CORRUPTED ACK RECEIVED");
            numCorrupt++;
            return;
        }
        
        // If ACK is not corrupted but out-dated, slide window
        while(seqBase < Psent.size()){
            
            if(Psent.get(seqBase).getSeqnum() == packet.getAcknum()){ //ACK is right for the last sent packet
                break;
            }
            
            Ttime.add(seqBase, time - Ttime.get(seqBase)); //Each ACK is recorded in the linked list for next check
            seqBase++;
            seqLast++;
            
        }
        
        //If ACK is not corrupted and not out-dated, slide window for only one position
        Ttime.add(seqBase, time - Ttime.get(seqBase));
        seqBase++;
        seqLast++;
        
        // If there are no outstanding packets, stop timer 
        if(seqBase == nextPacket){
            stopTimer(A);
            if((nextPacket <= seqLast)){
                if (nextPacket < Psent.size()){
                    
                    Double t = getTime();
                    toLayer3(A, Psent.get(nextPacket));
                    System.out.println("A:sending packet.");
                    if(seqBase == nextPacket){ 
                        startTimer(A, RxmtInterval); 
                    }
                    
                    //If the window has room, it will send the next packet waiting to be sent
                    nextPacket++;
                    totalSentPacket++;
                    Ttime.add(nextPacket, t);
                }
            }
        }else{
            stopTimer(A);
            startTimer(A, RxmtInterval);
        }
        
    }
    
    // This routine will be called when A's timer expires (thus generating a 
    // timer interrupt). You'll probably want to use this routine to control 
    // the retransmission of packets. See startTimer() and stopTimer(), above,
    // for how the timer is started and stopped. 
    protected void aTimerInterrupt()
    {
        //record receiving time
        double time = getTime();
        
        System.out.println("A:Timer interrupt!");
        startTimer(A, RxmtInterval);
        
        // Retransmit window
        for(int i = seqBase; ((i <= seqLast) &&  (i < Psent.size())); i++){           
            Packet p = Psent.get(i);
            toLayer3(A, p); // keep sending packets
            totalSentPacket++;
            
            if(i == nextPacket){ //First time sending a specfic packet
                Ttime.add(i, time);
                nextPacket++;
            }
            System.out.println("A:Retransmiting packet#"+i);
        }
    }
    
    // This routine will be called once, before any of your other A-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity A).
    protected void aInit()
    {
        System.out.println("Initializing A.");
        seqNum = FirstSeqNo;
        seqBase = FirstSeqNo;
        seqLast = FirstSeqNo + WindowSize - 1;
        
        nextPacket = FirstSeqNo;
        
        totalSentPacket = 0;
        numCorrupt = 0;
        numACK = 0;
        Psent = new LinkedList<Packet>();
        Ttime = new LinkedList<Double>(); 
    }
    
    // This routine will be called whenever a packet sent from the A-side 
    // (i.e. as a result of a toLayer3() being done by an A-side procedure)
    // arrives at the B-side.  "packet" is the (possibly corrupted) packet
    // sent from the A-side.
    protected void bInput(Packet packet)
    {
        System.out.println("B:received packet.");
        totalDeliveredPacket++;
        
        // If packet is corrupted, timeout
        if(checkSum(packet) != packet.getChecksum()){
            System.out.println("B:CORRUPTED PACKET RECEIVED");
            numCorrupt++;
            return; 
        }
        
        
        if(packet.getSeqnum() == seqExpected){
            
            // If packet is not corrupted, send ACK to A
            Packet ACK = new Packet(packet.getSeqnum(), seqExpected, -1, ""); // create packet
            ACK.setChecksum(checkSum(ACK)); //calculate the checksum and set
            System.out.println("B:Sending ACK #"+ ACK.getAcknum());
            toLayer3(B, ACK);    // Call to layer 3, send ACK to A
            numACK++;
            
            seqExpected = updateSeq(seqExpected); // update next expected sequence number
            
            toLayer5(packet.getPayload()); // sending message to layer 5
            
        }else{
            //If packet is not corrupted but out-dated, discard it and for retransmitting
            System.out.println("B:received out-dated packet, discard it");
        }
    }
    
    // This routine will be called once, before any of your other B-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity B).
    protected void bInit()
    {
        System.out.println("Initializing B.");
        seqExpected = FirstSeqNo;
        totalDeliveredPacket = 0;
    }
    
    // Use to print final statistics
    protected void Simulation_done()
    {
        System.out.println(" ");
        System.out.println("Simulation Statistic:");
        System.out.println("Total packets sent = " + totalSentPacket);
        System.out.println("Window Size: " + WindowSize);     
        System.out.println("----------------------------------- ");
        
        System.out.println("number of packets lost: " + nLost);
        System.out.println("percent of packets lost: " + ((double) (nLost)/totalSentPacket));
        System.out.println("number of packets corrupted: " + nCorrupt);
        System.out.println("percent of packets corruption: " + ((double) nCorrupt/(totalSentPacket-nLost)));
        System.out.println("----------------------------------- ");
        
        System.out.println("number of original data packets sent: " + nextPacket);
        System.out.println("number of ACK packets sent: " + numACK);
        System.out.println("number of retransmissions: " + (totalSentPacket - nextPacket)); 
        System.out.println("----------------------------------- ");
        
        System.out.println("Average RTT for packets: " + averageRTT());
        
    } 
}
