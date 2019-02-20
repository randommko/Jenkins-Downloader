package sample;


import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.lang.String;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;


public class Controller
{

    @FXML
    private volatile Label statusLabel;
    @FXML
    private TextArea logTextArea;
    @FXML
    private TextField serverAddressTextField, downloadPathTextField;
    @FXML
    private ListView<String> jobListView = new ListView<>();
    @FXML
    private Button refreshButton, downloadButton, chooseDirectoryButton;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private SplitPane splitPane;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private HBox topHBox, midHBox, botHBox;
    @FXML
    private VBox vBOx;
    @FXML
    private AnchorPane root;
    @FXML
    private Label statusLable, serverAddrlabel, logLabel;

    private String serverAddress;
    private ObservableList<JenkinsJobs> ListOfJobs = FXCollections.observableArrayList ();
    private int JobCounter = 0;
    private int __width, __height;
    private DirectoryChooser directoryChooser;
    private Main main;
    private enum ClientStatus {Disconnected, Connected, Downloading, Extracting, Connecting, Updating}
    public  enum JobStatusListing {built,  Успешно, Провалилось, Прервано, Приостановлено, процессе, Неизвестно, Ошибка}
    private static final javafx.scene.image.Image okImage = new Image("/image/ok.png", true);
    private static final javafx.scene.image.Image cancelImage = new Image("/image/cancel.png", true);

    private List<jobStatus> jobsStatusList;
    private Thread threadUpdateStatusOfJobs;
    private Thread threadGetJobList;

    @FXML
    private void initialize()                   //метод в котором выполняется код при запуске приложения
    {
        initWindow();

        jobListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        jobsStatusList = new ArrayList<>();

        setStatus(ClientStatus.Connecting);
        connectToServer();

        Runnable updateStatus = () -> {
            String out;

            SimpleDateFormat formatForDateNow = new SimpleDateFormat("HH:mm:ss");

            do {
                try {
                    out = refreshJobStatus();   //обновляем статусы всех работ. Возврашает строку с навзаниями работ у которых статус изменился

                    Date date = new Date();
                    System.out.println(formatForDateNow.format(date) + ": " + out);

                    if (!out.equals("No jobs has been updated"))
                        writeToLog("This jobs has been updated:\n" + out);
                    TimeUnit.SECONDS.sleep(5);
                }
                catch (Exception e) {
                    System.out.println("Error on job status updating: " + e);
                }
            } while (true);

        };

        threadUpdateStatusOfJobs = new Thread(updateStatus);
        threadUpdateStatusOfJobs.start();
    }

    @FXML
    private void connectToServer()              //нажатие кнопки "Refresh"
    {
        setStatus(ClientStatus.Updating);

        Runnable runnableGetJobList = () -> {
            try {
                int numOfFoundJobs = getJobListFromServer();
                writeToLog("Available jobs found: " + numOfFoundJobs);  //Получение спика работ в текущем потоке
                setStatus(ClientStatus.Connected);
            }
            catch (IOException e) {
                writeToLog("Server not found!");
                setStatus(ClientStatus.Disconnected);
            }
        };

        threadGetJobList = new Thread(runnableGetJobList);

        jobListView.getItems().remove(0, jobListView.getItems().size());
        JobCounter = 0;
        ListOfJobs.remove(0, ListOfJobs.size());

        serverAddress = serverAddressTextField.getText();   //запоминаем адресс сервера
        System.out.println("Server address: " + serverAddress);

        threadGetJobList.start();
    }

    @FXML
    private void downloadJobButton ()           //Нажатие на кнопку "Download" и начало скачивания в новом потоке
    {
        setStatus(ClientStatus.Downloading);

        String jobName = jobListView.getSelectionModel().getSelectedItems().toString();
        JenkinsJobs job;
        int temp = 0;
        for (int i = 0; i < JobCounter; i++) {
            if (jobName.indexOf(ListOfJobs.get(i).getJobName()) > 0) {
                temp = i;
            }
        }
        job = ListOfJobs.get(temp);

        File folder = new File(downloadPathTextField.getText() + "\\" + job.getJobName() + "\\");

        if (!folder.exists()) {
            folder.mkdirs();
        }
        File file = new File(folder, job.getJobID() + ".zip");

        double lastSize = ((getLastSize(folder) / 1024) / 1024);
        lastSize = lastSize * 100;
        lastSize = (int) lastSize;
        lastSize = lastSize / 100;

        double _lastSize = lastSize;

        Runnable runnable = () -> {
            if (!file.exists() && !file.isFile()) {

                if ((getLastSize(folder)) == -1)
                    writeToLog("Start downloading: " + job.getJobName() + " (" + job.getJobID() + ")");
                else
                    writeToLog("Start downloading: " + job.getJobName() + " (" + job.getJobID() + "), " + _lastSize + "Mb");
                downloadJob(file, job.getURL(), getLastSize(folder));  //скачивание
                setStatus(ClientStatus.Connected);
            }
            else {
                writeToLog(job.getJobName() + " (" + job.getJobID() + ") already exists");
                setStatus(ClientStatus.Connected);
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    private void downloadJob (File file, URL url, double size)   //скачивание
    {
        setStatus(ClientStatus.Downloading);
        progressBar.setProgress(0);
        FileOutputStream fileoutputstream;
        InputStream inputstream;

        try {
            fileoutputstream = new FileOutputStream(file);
            inputstream = url.openStream();

            if(file.exists()) {
                if(file.isDirectory())
                    throw new IOException((new StringBuilder()).append("File '").append(file).append("' is a directory").toString());
                if(!file.canWrite())
                    throw new IOException((new StringBuilder()).append("File '").append(file).append("' cannot be written").toString());
            }
            else {
                File file1 = file.getParentFile();
                if(file1 != null && !file1.exists() && !file1.mkdirs())
                    throw new IOException((new StringBuilder()).append("File '").append(file).append("' could not be created").toString());
            }

            if(size == -1) {
                progressBar.setVisible(false);
            }

            byte abyte0[] = new byte[4096];
            for (int j = 0; -1 != (j = inputstream.read(abyte0));) {
                fileoutputstream.write(abyte0, 0, j);
                if (size == -1)
                    progressBar.setProgress(0);
                else
                    progressBar.setProgress((file.length())/size);
            }

            inputstream.close();
            fileoutputstream.close();
            writeToLog("Download complete");

            unzip(file);
        }
        catch (java.lang.RuntimeException e) {
            System.out.println("Ошибка: " + e);
        }
        catch (IOException e) {
            System.out.println("Ошибка: " + e);
        }
        writeToLog("Extract complete");
    }

    public void unzip(File file)
    {
        String s = file.getPath();
//        System.out.println("Path: " + s);
//        System.out.println(s);
        String s1 = s.substring(0, s.length() - 4);
        try {
            ZipFile zipfile = new ZipFile(file);
            zipfile.extractAll(s1);
        }
        catch(ZipException zipexception) {
            zipexception.printStackTrace();
        }
    }

    @FXML
    public void setDirectory ()            //нажатие на кнопку "Выбрать директорию..."
    {
        directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Выберете папку для сохранения");

        try {
            File selectedDirectory = directoryChooser.showDialog(main.getStage());
            downloadPathTextField.setText(selectedDirectory.getAbsolutePath());
        }
        catch (NullPointerException e) {
            System.out.println("No directory: " + e);
        }
    }

    private int getJobListFromServer()  //Получение списка работ
            throws IOException
    {
        System.out.println("Getting job list from server...");
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

            try {
                inputstream = url.openStream(); //пробуем открыть сформированную ссылку, если не получится то мы сразу попадаем в блок catch, иначе выполнение кода продолжится дальше и мы добавим работу в наш список

                ListOfJobs.add(new JenkinsJobs(++JobCounter, jobName, jobID, url, getJobStatus(element)));    //создаем новую джобу с полученым с сервера именем, добавляем джобу в список и увеличиваем счетчик
                //System.out.println("Found job: " + ListOfJobs.get(JobCounter - 1).getJobName() + ": " + ListOfJobs.get(JobCounter - 1).getJobStatus());
                Platform.runLater(
                        () -> {
                            jobListView.getItems().add(ListOfJobs.get(JobCounter - 1).getJobName() + " (" + ListOfJobs.get(JobCounter - 1).getJobID() + ")");    //добавляем найденую работу в элмент ListView
                        }
                );

                inputstream.close();

            }
            catch (FileNotFoundException e) {
                //System.out.println("File not found for: " + jobName);
            }
            catch (IOException e) {
                //System.out.println("IOError: " + e);
            }
            catch (Exception e) {
                //System.out.println("Error: " + e);
            }

        }
        //return changedJobs;
        return ListOfJobs.size();
    }

    private String refreshJobStatus()
    {
        System.out.print("Refreshing job status... ");
        long startTime = System.currentTimeMillis();
        String jobUpdated = "";
        List<jobStatus> oldJobsStatusList = jobsStatusList;
        jobsStatusList.clear();

        try {
            Document document = Jsoup.connect(serverAddress).get(); //получаем копию страницы в виде документа
            //System.out.println(document);
            Elements elements = document.select("tr[class*=job-status]");   //создаем список tr-элементов страницы которые содержат текст "job-status" внутри себя
            //Elements elementsTD = document.select("[alt~=[Успешно|Провалилось|Прервано|Приостановлено|процессе|built]]");   //создаем список всех элеметов страницы которые содержат одно из перечисленных слов-состояний

            //Iterator statusIterator = elementsTD.iterator();            //создаем итератор по элементам страницы содержащим состояния работ
            Iterator iteratorListOfJobs = elements.iterator();          //создаем итератор по элментам страницы содержащим имена работ

            while ( iteratorListOfJobs.hasNext() ) {
                Element element = (Element) iteratorListOfJobs.next();                  //берем следующую работу
                String jobName = element.attr("id");                         //находим строку начинающуюся с "id"
                jobName = jobName.substring(4, jobName.length());                       //берем символы с 4 до последнего, это и есть имя работы
                //Element elementTD = (Element) statusIterator.next();                    //берем следующий элемент состояния работы
                jobsStatusList.add(new jobStatus(jobName, getJobStatus(element)));    //добавляем сочетание "имя_работы + статус_сборки" в список
            }
            //Сравнение старого и нового списка
            Iterator newStatusIterator = jobsStatusList.iterator();

            while (newStatusIterator.hasNext()) {
                jobStatus newStatus = (jobStatus) newStatusIterator.next();
                //System.out.println("NEW. Job name: " + newStatus.getJobName() + ", status: " + newStatus.getJobStatus());
                Iterator oldStatusIterator = oldJobsStatusList.iterator();
                while (oldStatusIterator.hasNext()) {
                    jobStatus oldStatus = (jobStatus) oldStatusIterator.next();
                    //System.out.println("OLD. Job name: " + oldStatus.getJobName() + ", status: " + oldStatus.getJobStatus());
                    if (newStatus.getJobName().equals(oldStatus.getJobName())) {
                        if (!newStatus.equals(oldStatus))
                            jobUpdated = jobUpdated + newStatus.getJobName() + " changed status to: " + newStatus.getJobStatus() + "\n";
                        break;
                    }
                }
            }
        }
        catch (IOException e) {
            System.out.println("Error on refreshing jobs status: " + e);
        }
        long endTime = System.currentTimeMillis();
        double time = (endTime - startTime);
        time = time / 1000;
        System.out.println(time + " sec");

        if (jobUpdated.equals(""))
            jobUpdated = "No jobs has been updated";

        return jobUpdated;
    }

//---------------------вывод в лог/статус бар---------------------

    private void setStatus (ClientStatus status)    //установка статуса клиента (приложения)
    {
        try
        {
            switch (status)
            {
                case Connecting:

                    setStatusText("Connecting...", Color.GREEN);
                    progressIndicator.setVisible(true);
                    progressBar.setVisible(false);
                    downloadButton.setDisable(true);
                    refreshButton.setDisable(true);
                    break;
                case Connected:
                    setStatusText("Connected", Color.GREEN);
                    progressIndicator.setVisible(false);

                    progressBar.setVisible(false);
                    downloadButton.setDisable(false);
                    refreshButton.setDisable(false);
                    break;
                case Updating:

                    setStatusText("Updating...", Color.GREEN);
                    progressIndicator.setVisible(true);
                    progressBar.setVisible(false);
                    downloadButton.setDisable(true);
                    refreshButton.setDisable(true);
                    break;
                case Extracting:

                    setStatusText("Extracting...", Color.GREEN);
                    progressIndicator.setVisible(true);
                    progressBar.setVisible(false);
                    downloadButton.setDisable(true);
                    refreshButton.setDisable(true);
                    break;
                case Downloading:

                    setStatusText("Downloading...", Color.GREEN);
                    progressIndicator.setVisible(true);
                    progressBar.setVisible(true);
                    downloadButton.setDisable(true);
                    refreshButton.setDisable(true);
                    break;
                case Disconnected:
                    progressIndicator.setVisible(true);
                    progressBar.setVisible(false);
                    setStatusText("Disconnected", Color.RED);
                    progressIndicator.setVisible(false);

                    downloadButton.setDisable(false);
                    refreshButton.setDisable(false);
                    break;
                default:
                    progressIndicator.setVisible(true);
                    progressBar.setVisible(false);
                    setStatusText("Status error", Color.RED);
                    progressIndicator.setVisible(false);

                    refreshButton.setDisable(true);
                    downloadButton.setDisable(true);
                    break;
            }
        }
        catch (Exception e)
        {
            System.out.println("Error on change status: " + e);
        }

    }

    public void writeToLog(String text)     //вывод в лог сообщения
    {
        Date date = new Date();
        SimpleDateFormat formatForDateNow = new SimpleDateFormat("dd.MM.yy HH:mm:ss");

        try
        {
            logTextArea.setText(logTextArea.getText() + formatForDateNow.format(date) + ": " + text + "\n");
        }
        catch (Exception e)
        {
            System.out.println("Error on write to log: " + e);
        }
    }

    public Controller.JobStatusListing getJobStatus(Element element)    //получения статуса работы по элементу
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
                    return JobStatusListing.Успешно;
                case "Провалилось":
                    return JobStatusListing.Провалилось;
                case "Прервано":
                    return JobStatusListing.Прервано;
                case "Приостановлено":
                    return JobStatusListing.Приостановлено;
                case "процессе":
                    return JobStatusListing.процессе;
                case "built":
                    return JobStatusListing.built;
                default:
                    return JobStatusListing.Неизвестно;
            }
        }
        return JobStatusListing.Ошибка;
    }

    //---------------------мелкие функции---------------------

    private void initWindow()
    {
        main = new Main();

        __width = main.getSCENE_WIDTH();
        __height = main.getSCENE_HEIGHT();

        logTextArea.setEditable(false);

        topHBox.setMinHeight(25);
        midHBox.setMinHeight(150);
        botHBox.setMinHeight(25);

        final int minWidth  = 750;
        final int minHeight = 300;

        topHBox.setMinWidth(minWidth);
        midHBox.setMinWidth(minWidth);
        botHBox.setMinWidth(minWidth);

        root.setMinHeight(minHeight);
        root.setMinWidth(minWidth);
    }

    private double getLastSize(File folder)
    {
        double size = -1;
        File[] folderEntries = folder.listFiles();
        for (File entry : folderEntries) {
            if (entry.isDirectory()) {
                continue;
            }
            size = entry.length();
        }

        return size;
    }

    public void addJobToLstOfJob (JenkinsJobs job) //новую джобу с полученым с сервера именем добавляем в список и увеличиваем счетчик
    {
        ListOfJobs.add(job);
        JobCounter++;
    }

    public int getIntJobID(String stringJobID)
    {
        int jobID = -1;

        try {
            jobID = Integer.parseInt(stringJobID);
//            System.out.println("jobID (int) = " + jobID);
        }
        catch (NumberFormatException e) {
//            System.out.println("Error in getting job ID (int): " + e);
        }

        return jobID;
    }

    public void setStatusText (String text, Color color)
    {
        try {
            Platform.runLater(
                    () -> {
                        statusLabel.setText(text);
                        statusLabel.setTextFill(color);
                    }
            );
        }
        catch (Exception e) {
            System.out.println("Label error: " + e);
        }
    }

    public ListView<String> getJobList()
    {
        return jobListView;
    }

    public  ObservableList<JenkinsJobs> getListOfJobs()
    {
        return ListOfJobs;
    }

    public int      getJobCounter()
    {
        return JobCounter;
    }

    public void     setJobCounter(int num)
    {
        JobCounter = JobCounter + num;
    }

    public String   getServerAddress()
    {
        return serverAddress;
    }

    public void     setServerAddress(String addr)
    {
        serverAddress = addr;
    }
}
