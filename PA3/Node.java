import java.io.*;
//import java.util.Array;
import java.util.ArrayList;

/*
 * Ding Jin(dingj@bu.edu)
 * CS455 PA3
 * 11/22/2015
 * /
 * 
 /*
 * This is the class that students need to implement. The code skeleton is provided.
 * Students need to implement rtinit(), rtupdate() and linkhandler().
 * printdt() is provided to pretty print a table of the current costs for reaching
 * other nodes in the network.
 */ 
public class Node { 
    
    public static final int INFINITY = 9999;
    
    int[] lkcost;     /*The link cost between node 0 and other nodes*/
    int[][] costs;    /*Define distance table*/
    int nodename;     /*Name of this node*/
    int[] routes;     /*Define routing table*/
    int node_num = 4; /*declare the distance table as a 4-by-4 array*/
    
    /* Class constructor */
    public Node() {
        lkcost = new int[node_num];
        costs = new int[node_num][node_num];
        routes = new int[node_num];
    }
    
    
    /* 
     * Initializing Wrapper Structure:
     * --Initialize the distance table
     * --Initialize the routes table
     * --Send the shortest path info to neighbours
     */
    void rtinit(int nodename, int[] initial_lkcost) {
        System.out.println("Initializing Node "+ nodename);
        System.out.println("==================================");
        
        this.nodename = nodename;
        lkcost = initial_lkcost;
        
        //--Initialize the distance and route table
        init_d(costs, initial_lkcost);
        init_r(routes, lkcost);
        
        //--Send the shortest path info to neighbours
        inform_neighbour();
    }
    
    
    /*
     * Update Wrapper Structure:
     * --Update the costs table
     * --Compute the least link cost and update the route table
     * --Check if the cost array has been updated
     * --Inform neighbour
     * --(optional)send poison packets
     */
    void rtupdate(Packet rcvdpkt) {
        int sourceid = rcvdpkt.sourceid;
        int[] mincost = rcvdpkt.mincost;
        
        int [][]prevcosts = new int[node_num][node_num];
        for (int i = 0; i < prevcosts.length; i++) {
            for (int j = 0; j < prevcosts[0].length; j++) {
                prevcosts[i][j] = costs[i][j];
            }
        }
        
        //--Update the costs table
        update_d(sourceid, mincost);
        
        //--Compute the least link cost and update the route table
        int[] new_mincost = new int[lkcost.length];
        new_mincost = update_r();
        
        //--Check if node is changed
        if (is_updated(prevcosts, costs)) {
            System.out.println("Table has been updated");
            printdt();
            printnode();
            //--Inform neighbour
            //--(optional)Send poison packets
            poison_inform(new_mincost);
        } else {
            System.out.println("Table not updated");
            System.out.println("==================================");
        }
    }
    
    
    /* 
     * LINKCHANGED version
     * --Simulate the costs array
     * --Compute the least link cost and update the route table
     * --Inform neighbour(send poison packet if available)
     */
    void linkhandler(int linkid, int newcost) {
        lkcost[linkid] = newcost;
        
        //--Simulate the costs array 
        for (int i = 0; i < node_num; i++) {
            costs[i][linkid] = INFINITY;
        }
        
        //--Compute the least link cost and update the route table
        int[] mincost = new int[lkcost.length];
        mincost = update_r();
        
        //--Inform neighbours
        poison_inform(mincost);
    }
    
    /*
     * helper methods:
     */
    
    //Traverse the 4*4 spray costs table and fill with either inital_link_cost or infinity
    void init_d (int[][] costs, int[] ilk){
        for (int i = 0; i < node_num; i++) {
            for (int j = 0; j < node_num; j++) {
                if (i == j) {
                    costs[i][j] = ilk[i];
                } else {
                    costs[i][j] = INFINITY;
                }
            }
        }
    }
    
    //Initialzation of route table
    //The situation when inital link cost is infinity has preference
    void init_r (int[] routes, int[] lk) {
        for (int i = 0; i < node_num; i++) {
            routes[i] = -1;
            if (lk[i] != INFINITY) {
                routes[i] = i;
            }
        }
    }
    
    // Check if the costs array has been updated
    boolean is_updated(int[][] previous, int[][] current) {
        for (int i = 0; i < previous.length; i++) {
            for (int j = 0; j < previous[0].length; j++) {
                if (previous[i][j] != current[i][j]) {
                    return true;
                }    
            }
        }
        return false;
    }
    
    //send costs of its shortest paths to all other network nodes to neighbours
    void inform_neighbour(){
        //simulate the mincost array
        int [] mincost = new int[lkcost.length];
        for (int i = 0; i < mincost.length; i++) {
            mincost[i] = costs[i][i];
        }
        
        //actuall sending part
        for (int i = 0; i < lkcost.length; i++) {
            if (i != this.nodename && lkcost[i] != INFINITY) {
                NetworkSimulator.tolayer2(new Packet(this.nodename, i, mincost));
            }
        }    
    }
    
    //update the costs table according to the packet's sourceid
    //
    void update_d (int sourceid, int[] mincost){
        for (int i = 0; i < mincost.length; i++) {        
            if (mincost[i] == INFINITY) { costs[i][sourceid] = INFINITY; } 
            else { costs[i][sourceid] = lkcost[sourceid] + mincost[i]; }
        }
    }
    
    //compute the new mincost(Packet.mincost) and update the route table.
    int[] update_r (){
        int[] new_mincost = new int[lkcost.length];
        for (int dest = 0; dest < new_mincost.length; dest++) {
            int min_temp = INFINITY;
            int index = -1;
            //fill out the mincost row with the next hops
            for (int i = 0; i < costs[dest].length; i++) {
                if (costs[dest][i] < min_temp) {
                    min_temp = costs[dest][i];
                    index = i;
                }
            }
            new_mincost[dest] = min_temp;
            routes[dest] = index;
        }
        return new_mincost;
    }
    
    // Inform the neighbour with the Split Horizon with Poison Reverse heuristic
    void poison_inform(int[] m) {
        for (int i = 0; i < lkcost.length; i++) {
            if (i != this.nodename && lkcost[i] != INFINITY) {
                int[] poison_mincost = new int[m.length];
                for (int j = 0; j < poison_mincost.length; j++) {
                    poison_mincost[j] = m[j];
                }
                
                //update mincost according to split horizon with poison reverse
                for (int k = 0; k < poison_mincost.length; k++) {
                    if (routes[k] == i) {
                        poison_mincost[k] = INFINITY;
                    }
                }
                NetworkSimulator.tolayer2(new Packet(this.nodename, i, poison_mincost));
            }
        }
    }
    
    
    // Prints the current node routing table.
    //hopping situation + link cost
    void printnode() {
        System.out.println("Node "+ this.nodename +" Routing Situation:");
        System.out.print("1-->" + routes[0]+"(cost:"+lkcost[0]+"), ");
        System.out.print("2-->" + routes[1]+"(cost:"+lkcost[1]+"), ");
        System.out.print("3-->" + routes[2]+"(cost:"+lkcost[2]+"), ");
        System.out.print("4-->" + routes[3]+"(cost:"+lkcost[3]+")");
        System.out.println("");
        System.out.println("==================================");
    }
    
    /* Prints the current costs to reaching other nodes in the network */
    void printdt() {
        switch(nodename) {
            case 0:
                System.out.printf("                via     \n");
                System.out.printf("   D0 |    1     2    3 \n");
                System.out.printf("  ----|-----------------\n");
                System.out.printf("     1|  %3d   %3d   %3d\n",costs[1][1], costs[1][2],costs[1][3]);
                System.out.printf("dest 2|  %3d   %3d   %3d\n",costs[2][1], costs[2][2],costs[2][3]);
                System.out.printf("     3|  %3d   %3d   %3d\n",costs[3][1], costs[3][2],costs[3][3]);
                break;
            case 1:
                System.out.printf("                via     \n");
                System.out.printf("   D1 |    0     2 \n");
                System.out.printf("  ----|-----------------\n");
                System.out.printf("     0|  %3d   %3d \n",costs[0][0], costs[0][2]);
                System.out.printf("dest 2|  %3d   %3d \n",costs[2][0], costs[2][2]);
                System.out.printf("     3|  %3d   %3d \n",costs[3][0], costs[3][2]);
                break;
                
            case 2:
                System.out.printf("                via     \n");
                System.out.printf("   D2 |    0     1    3 \n");
                System.out.printf("  ----|-----------------\n");
                System.out.printf("     0|  %3d   %3d   %3d\n",costs[0][0], costs[0][1],costs[0][3]);
                System.out.printf("dest 1|  %3d   %3d   %3d\n",costs[1][0], costs[1][1],costs[1][3]);
                System.out.printf("     3|  %3d   %3d   %3d\n",costs[3][0], costs[3][1],costs[3][3]);
                break;
            case 3:
                System.out.printf("                via     \n");
                System.out.printf("   D3 |    0     2 \n");
                System.out.printf("  ----|-----------------\n");
                System.out.printf("     0|  %3d   %3d\n",costs[0][0],costs[0][2]);
                System.out.printf("dest 1|  %3d   %3d\n",costs[1][0],costs[1][2]);
                System.out.printf("     2|  %3d   %3d\n",costs[2][0],costs[2][2]);
                break;
        }
    }
}//end