package sample;

import javafx.concurrent.Task;

public class myTask extends Task<Void>

{
    String text;
    myTask(String text)
    {
        this.text = text;
    }

    @Override
    protected Void call()
    {
        updateMessage(text);
        return null;
    }
}
