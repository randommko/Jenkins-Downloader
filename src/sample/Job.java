package sample;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Job {

    public enum JobStatusListing {built,  Успешно, Провалилось, Прервано, Приостановлено, Впроцессе, Неизвестно, Ошибка}


    private String visibleName; //отображемое имя (необязательно)
    private int jobID;          //номер последней сборки
    private String jobName ;    //имя джобы
    private URL jobURL;         //ссылка на скачивание последнего успешного билда
    private JobStatusListing jobStatus; //статус последней сборки
    private boolean isFile;
    private String lastChange;

    public Job (String jobName, int jobID, URL jobURL, boolean isFile, JobStatusListing jobStatus)
    {
        this.jobName        = jobName ;
        this.jobID          = jobID;
        this.jobURL         = jobURL;
        this.jobStatus      = jobStatus;
        this.isFile         = isFile;
        this.visibleName    = "";
//        SimpleDateFormat formatForDateNow = new SimpleDateFormat("dd.MM.yy HH:mm:ss");
//        Date date = new Date();
//        this.lastChange     = formatForDateNow.format(date);
        this.lastChange     = "Unknown";
    }

    public Job (Job job)
    {
        this.jobName        = job.getJobName();
        this.jobID          = job.getJobID();
        this.jobURL         = job.getJobURL();
        this.jobStatus      = job.getJobStatus();
        this.isFile         = job.isFile();
        this.visibleName    = job.getVisibleName();
        this.lastChange     = job.getLastChange();
    }

    //getters
    public String getJobName() {
        return jobName;
    }

    public String getVisibleName() {
        return visibleName;
    }

    public int getJobID() {
        return jobID;
    }

    public JobStatusListing getJobStatus()
    {
        return jobStatus;
    }

    public URL getJobURL() {
        return jobURL;
    }

    public boolean isFile() {
        return isFile;
    }

    public String getFileForTable() { return isFile? "yes" : "no"; }    //функция нужна для отображения в таблице

    public String getLastChange() {
        return lastChange;
    }

    public void setVisibleName(String visibleName) {
        this.visibleName = visibleName;
    }

    public void setIsFile(boolean flag) {
        isFile = flag;
    }

    public void setJobID(int jobID) {
        this.jobID = jobID;
        SimpleDateFormat formatForDateNow = new SimpleDateFormat("dd.MM.yy HH:mm:ss");
        Date date = new Date();
        this.lastChange     = formatForDateNow.format(date);
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public void setJobURL(URL jobURL) {
        this.jobURL = jobURL;
    }

    public void setJobStatus(JobStatusListing jobStatus) {
        this.jobStatus = jobStatus;
        SimpleDateFormat formatForDateNow = new SimpleDateFormat("dd.MM.yy HH:mm:ss");
        Date date = new Date();
        this.lastChange     = formatForDateNow.format(date);
    }

    public void setLastChange(String lastChange) {
        this.lastChange = lastChange;
    }

    public void download(File file)
    {
        FileOutputStream fileoutputstream;
        InputStream inputstream;

        try {
            fileoutputstream = new FileOutputStream(file);
            inputstream = jobURL.openStream();

            byte abyte0[] = new byte[4096];
            for (int j = 0; -1 != (j = inputstream.read(abyte0));)
                fileoutputstream.write(abyte0, 0, j);
            inputstream.close();
            fileoutputstream.close();

            unzip(file);
        }
        catch (IOException e) {
            System.out.println("Ошибка: " + e);
        }
    }

    private void unzip(File file)
    {
        String s = file.getPath();
        String s1 = s.substring(0, s.length() - 4);
        try {
            ZipFile zipfile = new ZipFile(file);
            zipfile.extractAll(s1);
        }
        catch(ZipException zipexception) {
            zipexception.printStackTrace();
        }
    }
}
