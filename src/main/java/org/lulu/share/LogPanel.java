package org.lulu.share;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogPanel extends JScrollPane{
    JTextArea outPutTextArea = new JTextArea();

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss" );

    public LogPanel() {
        //要先配置才能实现自动滚动
        outPutTextArea.setEnabled(false);
        //自动换行
        outPutTextArea.setLineWrap(true);
        outPutTextArea.setWrapStyleWord(true);
        outPutTextArea.setDisabledTextColor(Color.BLACK);
        setViewportView(outPutTextArea);
        //自动更新
        DefaultCaret caret = (DefaultCaret) outPutTextArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
    }

    public void i(String msg) {
        msg = msg.trim();
        if (msg.isEmpty()) {
            return;
        }
        Date date = new Date(System.currentTimeMillis());
        outPutTextArea.append(sdf.format(date) + " " + msg + "\n");
        System.out.println(msg);
    }

    public void e(String msg) {
        i("ERROR: " + msg);
    }


    public void clear() {
        outPutTextArea.setText("");
    }

}
