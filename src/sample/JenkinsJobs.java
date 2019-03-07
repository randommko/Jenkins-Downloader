package sample;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import static sample.AppSettings.findTagInConfigFile;

public class JenkinsJobs
{
    private ObservableList<Job> ListOfJobs = FXCollections.observableArrayList ();
    private MainController mainController;


    public JenkinsJobs()
    {
        mainController = new MainController();
    }

    public void clear()
    {
        this.ListOfJobs.clear();
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

    public void addJob(Job job)    //порядковый номер (полученный при поиске), имя, ИД, ссылка для скачивания
    {
        ListOfJobs.add(job);
    }

    public  int getJobListFromServer(String serverAddress)  //Получение списка работ
            throws IOException
    {
        System.out.println("(JenkinsJobs) Getting job list from server...");

        ListOfJobs.remove(0, ListOfJobs.size());                            //очищаем список работ

        Document document = Jsoup.connect(serverAddress).get(); //получаем копию страницы в виде документа

        Elements elements = document.select("tr[class*=job-status]");   //создаем список tr-элементов страницы которые содержат текст "job-status" внутри себя

        Iterator iteratorListOfJobs = elements.iterator();  //создаем итератор по элментам страницы содержащим имена работ

        while ( iteratorListOfJobs.hasNext() ) {
            Element element = (Element) iteratorListOfJobs.next();  //берем следующую работу
            String jobName = element.attr("id");         //находим строку начинающуюся с "id"
            jobName = jobName.substring(4, jobName.length());       //берем символы с 4 до последнего, это и есть имя работы

            String _s1 = element.child(3).text();
            int posNumber = _s1.indexOf("#");
            String stringJobID = _s1.substring(posNumber + 1);
            int jobID = getIntJobID(stringJobID);

            URL url =  new URL(serverAddress + "/view/actual/job/" + jobName + "/lastSuccessfulBuild/artifact/*zip*/archive.zip");  //формируем ВОЗМОЖНУЮ(!) ссылку на скачивание работы
            InputStream inputstream;
            Job job;
            try {
                inputstream = url.openStream(); //пробуем открыть сформированную ссылку, если не получится то мы сразу попадаем в блок catch, иначе выполнение кода продолжится дальше и мы добавим работу в наш список
                job = new Job(jobName, jobID, url, true, getJobStatusFromServer(element));  //создаем новую джобу с полученым с сервера именем, добавляем джобу в список и увеличиваем счетчик
                inputstream.close();
                if ( !(findTagInConfigFile(jobName)).equals(""))
                    job.setVisibleName(findTagInConfigFile(jobName));
            }
            catch (FileNotFoundException e) {
                job = new Job(jobName, jobID, url, false, getJobStatusFromServer(element));
                if ( !(findTagInConfigFile(jobName)).equals(""))
                    job.setVisibleName(findTagInConfigFile(jobName));
            }
            catch (Exception e) {
                job = new Job(jobName, jobID, url, false, getJobStatusFromServer(element));
                if ( !(findTagInConfigFile(jobName)).equals(""))
                    job.setVisibleName(findTagInConfigFile(jobName));
            }
            addJob(job);
            //mainController.addNewSettingToFile(job.getJobName(), job.getVisibleName());
        }
        return ListOfJobs.size();
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

            //{built,  Успешно, Провалилось, Прервано, Приостановлено, процессе, Неизвестно}
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

    public int getIntJobID(String stringJobID)
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
        System.out.print("Refreshing job status... ");

        long startTime = System.currentTimeMillis();
        String out = "";

        try {
            Document document = Jsoup.connect(address).get(); //получаем копию страницы в виде документа
            Elements elements = document.select("tr[class*=job-status]");   //создаем список tr-элементов страницы которые содержат текст "job-status" внутри себя

            Iterator iteratorListOfJobs = elements.iterator();          //создаем итератор по элментам страницы содержащим имена работ

            while ( iteratorListOfJobs.hasNext() ) {
                Element element = (Element) iteratorListOfJobs.next();                  //берем следующую работу
                String jobName = element.attr("id");                         //находим строку начинающуюся с "id"
                jobName = jobName.substring(4, jobName.length());                       //берем символы с 4 до последнего, это и есть имя работы

                String _s1 = element.child(3).text();
                int posNumber = _s1.indexOf("#");
                String stringJobID = _s1.substring(posNumber + 1);
                int jobID = getIntJobID(stringJobID);

                Job.JobStatusListing status = getJobStatusFromServer(element);

                for (int i = 0; i < ListOfJobs.size(); i++)
                {
                    Job job = ListOfJobs.get(i);

                    if ( job.getJobName().equals(jobName) )
                    {
                        if ( !job.getJobStatus().equals(status) )
                        {
                            ListOfJobs.get(i).setJobStatus(status);

                            if (ListOfJobs.get(i).getJobID() != jobID)
                                ListOfJobs.get(i).setJobID(jobID);
                            System.out.println("\n(JenkinsJobs)" + ListOfJobs.get(i).getJobName() + " changed status to: " + ListOfJobs.get(i).getJobStatus());

                            out = formationStringWithChangedJobs(out, ListOfJobs.get(i));
                        }
                        else
                            break;
                    }
                }
            }

            if (!out.equals(""))
                out = out.substring(0, out.length() - 1);   //если были изменения в статусах то убираем последний перенос строки
        }
        catch (IOException e) {
            System.out.println("(JenkinsJobs) Error on refreshing jobs status: " + e);
        }

        long endTime = System.currentTimeMillis();
        double time = (endTime - startTime);
        time = time / 1000;

        System.out.println(time + " sec");

        if (out.equals(""))
            out = "No jobs has been updated";

        SimpleDateFormat formatForDateNow = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();

        System.out.println(formatForDateNow.format(date) + ": " + out);

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
