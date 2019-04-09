package sample;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;


public class JenkinsJobs
{
    private ObservableList<Job> ListOfJobs = FXCollections.observableArrayList ();

    public void clear()
    {
        this.ListOfJobs.clear();
    }

    public JenkinsJobs()
    {

    }

    public JenkinsJobs(JenkinsJobs jobs)    //конструткор копирования
    {
        this.ListOfJobs.clear();

        Iterator iterator = jobs.getListOfJobs().iterator();

        while (iterator.hasNext())
        {
            Job job = (Job) iterator.next();
            this.ListOfJobs.add(job);
        }
    }

    public JenkinsJobs copyJobsList(JenkinsJobs inputJobs)   //возвращает копию переданого списка работ
    {
        return new JenkinsJobs(inputJobs);
    }

    private Iterator getJobsIterator(String serverAddress)
    {
        try
        {
            Document document = Jsoup.connect(serverAddress).get(); //получаем копию страницы в виде документа
            Elements elements = document.select("tr[class*=job-status]");   //создаем список tr-элементов страницы которые содержат текст "job-status" внутри себя
            return elements.iterator();  //создаем итератор по элментам страницы содержащим имена работ
        }
        catch (IOException e)
        {
            System.out.println("(JenkinsJobs) (getJobsIterator) Error: " + e);
            return null;
        }
    }

    public void getJobListFromServer(String serverAddress)  //Получение списка работ
    {
        System.out.println("(JenkinsJobs) (getJobListFromServer) Getting job list from server...");
        ListOfJobs.remove(0, ListOfJobs.size());                            //очищаем список работ
        Iterator iteratorListOfJobs = getJobsIterator(serverAddress);

        while ( iteratorListOfJobs.hasNext() ) {
            Element element = (Element) iteratorListOfJobs.next();  //берем следующую работу

            String jobName = getJobNameFromElement(element);
            int jobID = getJobIDFromElement(element);
            Job.JobStatusListing status = getJobStatusFromServer(element);

            ListOfJobs.add(new Job(jobName, jobID, status));
            //System.out.println("(JenkinsJobs) (getJobListFromServer) Job found: " + jobName);
        }
    }

    public Job.JobStatusListing getJobStatusFromServer(Element element)    //получения статуса работы по элементу
    {
        Elements statusElem = element.select("img");
        Iterator statusIterator = statusElem.iterator();

        while (statusIterator.hasNext())
        {
            String jobStatus;
            Element statusElement = (Element) statusIterator.next();
            jobStatus = statusElement.attr("alt");

            switch (jobStatus)    //далее нужно считать строку и выбрать соответствующий статус
            {
                case "Успешно":
                    return Job.JobStatusListing.Успешно;
                case "Провалилось":
                    return Job.JobStatusListing.Провалилось;
                case "Прервано":
                    return Job.JobStatusListing.Прервано;
                case "Приостановлено":
                    return Job.JobStatusListing.Приостановлено;
                case "В процессе":
                    return Job.JobStatusListing.Впроцессе;
                case "built":
                    return Job.JobStatusListing.built;
                default:
                    return Job.JobStatusListing.Неизвестно;
            }
        }
        return Job.JobStatusListing.Ошибка;
    }

    private String getJobNameFromElement(Element element)
    {
        String jobName = element.attr("id");                         //находим строку начинающуюся с "id"
        jobName = jobName.substring(4, jobName.length());                       //берем символы с 4 до последнего, это и есть имя работы
        return jobName;
    }

    private int getJobIDFromElement(Element element)
    {
        String _s1 = element.child(3).text();
        int posNumber = _s1.indexOf("#");
        String stringJobID = _s1.substring(posNumber + 1);
        return getIntJobID(stringJobID);
    }

    private int getIntJobID(String stringJobID)
    {
        int jobID = -1;
        try {
            jobID = Integer.parseInt(stringJobID);
        }
        catch (NumberFormatException e) {
            //System.out.println("Error in getting job ID (int): " + e);
        }
        return jobID;
    }

    public String refreshStatusOfAllJobs(String address)
    {
        String out = "";

        Iterator iteratorListOfJobs = getJobsIterator(address);

        while ( iteratorListOfJobs.hasNext() ) {
            Element element = (Element) iteratorListOfJobs.next();                  //берем следующую работу

            String jobName                  = getJobNameFromElement(element);
            int jobID                       = getJobIDFromElement(element);
            Job.JobStatusListing status     = getJobStatusFromServer(element);

            for (int i = 0; i < ListOfJobs.size(); i++)
            {
                Job job = ListOfJobs.get(i);

                if ( job.getJobName().equals(jobName) && (!job.getJobStatus().equals(status)) )
                {
                        ListOfJobs.get(i).setJobStatus(status);

                        if (ListOfJobs.get(i).getJobID() != jobID)
                            ListOfJobs.get(i).setJobID(jobID);
                        System.out.println("\n(JenkinsJobs)" + ListOfJobs.get(i).getJobName() + " changed status to: " + ListOfJobs.get(i).getJobStatus());


                        out = formationStringWithChangedJobs(out, ListOfJobs.get(i));
                        break;
                }
            }
        }

        if (!out.equals(""))
            out = out.substring(0, out.length() - 1);   //если были изменения в статусах то убираем последний перенос строки
        else
            out = "No jobs has been updated";           //иначе формируем строку для вывода в консоль

        Date date = new Date();
        SimpleDateFormat formatForDateNow = new SimpleDateFormat("dd.MM.yy HH:mm:ss");

        System.out.println(formatForDateNow.format(date) + "/ Status of auto update:" + ": " + out);

        return out;
    }

    private String formationStringWithChangedJobs(String actualString, Job job)
    {
        if ( job.getJobStatus() == Job.JobStatusListing.Впроцессе )   //Если джоба в процессе то не нужно отображать номер
            if (job.getVisibleName().equals(""))
                actualString = actualString + "\"" + job.getJobName() + "\" changed status to: " + "\"В процессе.\"" + "\n";
            else
                actualString = actualString + "\"" + job.getVisibleName() + "\" changed status to: " + "\"В процессе.\"" + "\n";
        else
            if (job.getVisibleName().equals(""))
                actualString = actualString + "\"" + job.getJobName() + " (#" + job.getJobID() + ")\" changed status to: \"" + job.getJobStatus() + "\"\n";
            else
                actualString = actualString + "\"" + job.getVisibleName() + " (#" + job.getJobID() + ")\" changed status to: \"" + job.getJobStatus() + "\"\n";

        return actualString;
    }

    public ObservableList<Job> getListOfJobs()
    {
        return ListOfJobs;
    }

}
