import java.io.Serializable;

public class Tasks implements Serializable, Comparable<Tasks>
{
    public Integer logicalClock = 0;
    public String content;

    //Parameterized constructor for PUT request
    public Tasks(int clock, String body)
    {
        this.logicalClock = clock;
        this.content = body;
    }

    //For use with Priority Blocking Queue
    @Override
    public int compareTo(Tasks c) 
    {
        return this.logicalClock.compareTo(c.logicalClock);
    }
}