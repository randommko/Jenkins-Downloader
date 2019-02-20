package sample;

import java.net.URL;

public class JenkinsJobs
{
    private final int jobNumber ;   //порядковый номер в списке всех найденых работ
    private final int jobID;        //номер последней сборки
    private final String jobName ;  //имя джобы
    private final URL jobURL;       //ссылка на скачивание последнего успешного билда
    private final Controller.JobStatusListing jobStatus; //статус последней сборки (пока нигде не используется)

    public JenkinsJobs(int jobNumber, String jobName, int jobID, URL jobURL, Controller.JobStatusListing jobStatus)    //порядковый номер (полученный при поиске), имя, ИД, ссылка для скачивания
    {
        this.jobNumber      = jobNumber ;
        this.jobName        = jobName ;
        this.jobID          = jobID;
        this.jobURL         = jobURL;
        this.jobStatus      = jobStatus;
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

//    public String getJobStatus()
//    {
//        try
//        {
//            switch (jobStatus) {
//                case built:
//                    return "build";
//                break;
//                case Успешно:
//                    return "Успешно";
//                break;
//                case Прервано:
//                    return "Прервано";
//                break;
//                case процессе:
//                    return "В процессе";
//                break;
//                case Неизвестно:
//                    return "Неизвестный статус";
//                break;
//                case Провалилось:
//                    return "Провалилось";
//                break;
//                case Приостановлено:
//                    return "Приостановленно";
//                break;
//                default:
//                    return "Error!";
//                break;
//
//            }
//        }
//        catch (Exception e)
//        {
//            System.out.println("Error on get job status: " + e);
//        }
//
//    }
}
