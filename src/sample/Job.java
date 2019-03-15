package sample;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import static sample.AppSettings.findTagInConfigFile;
import static sample.AppSettings.findTimeInConfigFile;

public class Job {

    public enum JobStatusListing {built,  Успешно, Провалилось, Прервано, Приостановлено, Впроцессе, Неизвестно, Ошибка}


    private String visibleName; //отображемое имя (необязательно)
    private int jobID;          //номер последней сборки
    private String jobName ;    //имя джобы
    private URL jobURL;         // serverAddress + /view/actual/job/ + jobName + /lastSuccessfulBuild/artifact/*zip*/archive.zip
    private JobStatusListing jobStatus; //статус последней сборки
    private boolean isFile;
    private String lastChange;
    private double size;    //размер джобы


    public Job (String jobName, int jobID, JobStatusListing jobStatus)
    {
        this.jobName        = jobName ;
        this.jobID          = jobID;
        this.jobStatus      = jobStatus;
        this.size           = AppSettings.findSizeInConfigFile(this.jobName);

        if ( !(findTagInConfigFile(jobName)).equals(""))
            this.visibleName = (findTagInConfigFile(jobName));   //ищем в конфиг-файле тэг для найденой работы
        else
            this.visibleName    = "";

        if ( !(findTimeInConfigFile(jobName)).equals(""))
            this.lastChange = (findTimeInConfigFile(jobName));
        else
            this.lastChange     = "-";

        try
        {
            this.jobURL = new URL(AppSettings.getServerAddress() + "/view/actual/job/" + jobName + "/lastSuccessfulBuild/artifact/*zip*/archive.zip");  //формируем ВОЗМОЖНУЮ(!) ссылку на скачивание работы
            InputStream inputstream = this.jobURL.openStream(); //пробуем открыть сформированную ссылку, если не получится то мы сразу попадаем в блок catch
            inputstream.close();
            this.isFile = true;
        }
        catch (Exception err)
        {
            this.isFile = false;
        }
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
        this.size           = job.getSize();
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
        AppSettings.changeSettingInConfig(jobName + "_time", this.lastChange);  //запись в конфиг времени последнего изменения
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public void setJobURL(URL jobURL) {
        this.jobURL = jobURL;
    }

    public double getSize() {
        return size;
    }

    public void setSize(double size) {
        this.size = size;
    }

    public void setJobStatus(JobStatusListing jobStatus) {
        this.jobStatus = jobStatus;
        SimpleDateFormat formatForDateNow = new SimpleDateFormat("dd.MM.yy HH:mm:ss");
        Date date = new Date();
        this.lastChange = formatForDateNow.format(date);
        AppSettings.changeSettingInConfig(jobName + "_time", this.lastChange);  //запись в конфиг времени последнего изменения
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

            this.size = file.length();
            AppSettings.changeSettingInConfig(this.jobName + "_size", String.valueOf(this.size));

            unzip(file);
        }
        catch (IOException e) {
            System.out.println("Ошибка: " + e);
        }
    }

    public void start()
    {
        try
        {
            URL url = new URL(AppSettings.getServerAddress() + "/job/" + this.jobName + "/build?delay=0sec");
            url.getContent();
        }
        catch (MalformedURLException e)
        {
            System.out.println("Malformed URL exception: " + e);

        }
        catch (IOException err)
        {
            System.out.println("URL IO Exception: " + err);
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
