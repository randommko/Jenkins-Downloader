package sample;


import com.thoughtworks.xstream.mapper.Mapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

public class jobFinder
        implements Runnable
{
    private Controller controller;
    private Document document;
    private String servAddr;


    jobFinder(String servAddr)
    {
        System.out.println("Запущен конструктор jobFinder'a");
        controller = new Controller();
        this.servAddr = servAddr;
    }

    private Document getDocument()
    {
        try
        {
            document = Jsoup.connect(servAddr).get();
        }
        catch (IOException e)
        {
            System.out.println("Error in 'getDocument()': " + e);
        }
        return document;
    }


    @Override
    public void run()
    //Этот метод будет выполняться в побочном потоке
    {
        System.out.println("Getting list of jobs in silent mode");

        Document document = getDocument();

        Elements elements = document.select("tr[class*=job-status]");
        Iterator iterator = elements.iterator();

        Elements elementsTD = document.select("[alt~=[Успешно|Провалилось|Прервано|Приостановлено|процессе|built]]");
//        System.out.println("elements_td = " + elementsTD);
        Iterator iteratoTD = elementsTD.iterator();

        int jobID;
        while ( iterator.hasNext() )
        {
            Element element = (Element) iterator.next();
            String jobName = element.attr("id");
            jobName = jobName.substring(4, jobName.length());
            String _s1 = element.child(3).text();

            Element elementTD = (Element) iteratoTD.next();

            int posNumber = _s1.indexOf("#");
            String stringJobID = _s1.substring(posNumber + 1);
            jobID = controller.getIntJobID(stringJobID);
            try
            {
                URL url =  new URL(servAddr + "/view/actual/job/" + jobName + "/lastSuccessfulBuild/artifact/*zip*/archive.zip");
                InputStream inputstream = url.openStream();
                if (inputstream.available() > 0)
                {
                    JenkinsJobs job = new JenkinsJobs(controller.getJobCounter(), jobName, jobID, url, true, controller.getJobStatus(elementTD));
                    controller.addJobToLstOfJob(job); //добавление джобы в список
                    //System.out.println("Adding job to ListView: " + job.getJobName());
                    //controller.addJobInListView(job); //добавление в ListView новой джобы

                }
                inputstream.close();
            }
            catch (IOException e)
            {
                try {
                    URL url =  new URL(servAddr + "/view/actual/job/" + jobName + "/lastSuccessfulBuild/artifact/*zip*/archive.zip");
                    JenkinsJobs job = new JenkinsJobs(controller.getJobCounter(), jobName, jobID, url, false, controller.getJobStatus(elementTD));
                    controller.addJobToLstOfJob(job);
                }
                catch (MalformedURLException err)
                {

                }

//                System.out.println("File not found for: " + jobName);
            }
        }

//        controller.writeToLog("Available jobs found: " + Integer.toString(controller.getListOfJobs().size()));
    }
}
