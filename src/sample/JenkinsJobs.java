package sample;

import java.net.URL;

public class JenkinsJobs
{
    private int jobNumber ;   //порядковый номер в списке всех найденых работ
    private int jobID;        //номер последней сборки
    private String jobName ;  //имя джобы
    private URL jobURL;       //ссылка на скачивание последнего успешного билда
    private Controller.JobStatusListing jobStatus; //статус последней сборки (пока нигде не используется)
    private boolean isFile;

    public JenkinsJobs(int jobNumber, String jobName, int jobID, URL jobURL, boolean isFile, Controller.JobStatusListing jobStatus)    //порядковый номер (полученный при поиске), имя, ИД, ссылка для скачивания
    {
        this.jobNumber      = jobNumber ;
        this.jobName        = jobName ;
        this.jobID          = jobID;
        this.jobURL         = jobURL;
        this.jobStatus      = jobStatus;
        this.isFile         = isFile;
    }

    public int getJobNumber()
    {
        return jobNumber ;
    }
    public String getJobName()
    {
        return jobName ;
    }
    public int getJobID()
    {
        return jobID;
    }
    public URL getURL()
    {
        return jobURL;
    }
    public Controller.JobStatusListing getJobStatus() {return jobStatus;}
    public boolean isFile() {
        return isFile;
    }

    public void setJobStatus (Controller.JobStatusListing status)
    {
        this.jobStatus = status;
    }
    public void setJobID (int ID)
    {
        this.jobID = ID;
    }

}
