package sample;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;

public class CasesReleaseDownloader
{
    private static final String JENKINS_URL = "http://nix.mrcur.ru:8080";
    private static final String CASES_RELEASE_ARTIFACTS_URL = "http://nix.mrcur.ru:8080/job/Cases_Release/lastSuccessfulBuild/artifact/Cases_Release_build/*zip*/Cases_Release_build.zip";
    private static final String JOB_NAME = "Cases_Release";   //имя скачиваемой джобы

    public CasesReleaseDownloader()
    {
    }

    private void getJobsList()
            throws IOException
    {
        Document document = Jsoup.connect("http://nix.mrcur.ru:8080").get();
        Elements elements = document.select("tr[class*=job-status]");
        Element element;
        for(Iterator iterator = elements.iterator(); iterator.hasNext(); System.out.println(element.attr("id")))
            element = (Element)iterator.next();

    }

    private int getLastSuccessfulBuildNumber(String s)
            throws IOException
    {
        Document document = Jsoup.connect("http://nix.mrcur.ru:8080").get();
        Element element = document.getElementById((new StringBuilder()).append("job_").append(s).toString());
        String s1 = element.child(3).text();
        int i = s1.indexOf("#");
        String s2 = s1.substring(i + 1);
        return Integer.parseInt(s2);
    }

    public void downloadArchive()
            throws IOException
    {
        System.out.println("Starting download. Job name: " + JOB_NAME);
        int i = getLastSuccessfulBuildNumber(JOB_NAME);
        File file = new File((new StringBuilder()).append(i).append(".zip").toString());
        URL url = new URL("http://nix.mrcur.ru:8080/job/" + JOB_NAME + "/lastSuccessfulBuild/artifact/Cases_Release_build/*zip*/Cases_Release_build.zip");
        try
        {
            InputStream inputstream = url.openStream();
            if(file.exists())
            {
                if(file.isDirectory())
                    throw new IOException((new StringBuilder()).append("File '").append(file).append("' is a directory").toString());
                if(!file.canWrite())
                    throw new IOException((new StringBuilder()).append("File '").append(file).append("' cannot be written").toString());
            } else
            {
                File file1 = file.getParentFile();
                if(file1 != null && !file1.exists() && !file1.mkdirs())
                    throw new IOException((new StringBuilder()).append("File '").append(file).append("' could not be created").toString());
            }
            FileOutputStream fileoutputstream = new FileOutputStream(file);
            byte abyte0[] = new byte[4096];
            for(int j = 0; -1 != (j = inputstream.read(abyte0));)
                fileoutputstream.write(abyte0, 0, j);

            inputstream.close();
            fileoutputstream.close();
            System.out.println((new StringBuilder()).append("File '").append(file).append("' downloaded successfully!").toString());
        }
        catch(IOException ioexception)
        {
            ioexception.printStackTrace();
        }
        unzip(file);
    }

    public void unzip(File file)
    {
        String s = file.getPath();
        System.out.println(s);
        String s1 = s.substring(0, s.length() - 4);
        try
        {
            ZipFile zipfile = new ZipFile(file);
            zipfile.extractAll(s1);
        }
        catch(ZipException zipexception)
        {
            zipexception.printStackTrace();
        }
    }


}
