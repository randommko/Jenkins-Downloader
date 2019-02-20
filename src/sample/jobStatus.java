package sample;

public class jobStatus
{

    private final String jobName ;  //имя джобы
    private Controller.JobStatusListing status;

    public jobStatus(String name, Controller.JobStatusListing status)
    {
        this.jobName = name;
        this.status = status;
    }

    public Controller.JobStatusListing getJobStatus()
    {
        return status;
    }

    public String getJobName()
    {
        return jobName;
    }

    public boolean equals (jobStatus status)
    {
        if (this.status == status.getJobStatus())
            return true;
        return false;
    }
}
