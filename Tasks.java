public class Tasks implements Comparable<Tasks>
{
    public Integer logicalClock = 0;
    public String content;
    public int clientID = 0;

    //Parameterized constructor for PUT request
    public Tasks(int clock, String body, int clientID)
    {
        this.logicalClock = clock;
        this.content = body;
        this.clientID = clientID;
    }

    //For use with Priority Blocking Queue
    @Override
    public int compareTo(Tasks c) 
    {
        return this.logicalClock.compareTo(c.logicalClock);
    }
}