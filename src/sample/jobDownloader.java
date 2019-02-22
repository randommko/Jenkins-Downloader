package sample;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.paint.Color;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import java.io.*;
import java.net.URL;

public class jobDownloader      //Нечто, реализующее интерфейс Runnable
        implements Runnable     //(содержащее метод run())
{

    private File file;
    private URL url;

    private Controller controller;

    jobDownloader (File file, URL url)
    {
        this.file = file;
        this.url = url;
        controller = new Controller();
    }

    @Override
    public void run()		//Этот метод будет выполняться в побочном потоке
    {
        controller.setStatusText("Downloading", Color.YELLOW);

        Platform.runLater(new Runnable() {
            @Override public void run() {
                controller.setStatusText("Downloading", Color.YELLOW);
            }
        });


        FileOutputStream fileoutputstream;
        InputStream inputstream;

        try
        {
            fileoutputstream = new FileOutputStream(file);
            inputstream = url.openStream();

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

            byte abyte0[] = new byte[4096];
            for (int j = 0; -1 != (j = inputstream.read(abyte0));)
                fileoutputstream.write(abyte0, 0, j);

            inputstream.close();
            fileoutputstream.close();

            controller.writeToLog("Download complete");

            controller.writeToLog("Extracting zip file...");
            unzip(file);
            controller.setStatusText("Connected", Color.GREEN);
        }
        catch (java.lang.RuntimeException e)
        {
            System.out.println("Ошибка: " + e);
        }
        catch (IOException e)
        {
            System.out.println("Ошибка: " + e);
        }
        controller.setStatusText("Connected", Color.GREEN);
    }

    public void unzip(File file)
    {
        String s = file.getPath();
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
