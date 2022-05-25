package org.lulu.share;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class GitShareFrame extends JFrame {

    public static final int PADDING = 10;

    public static final int W = 500;

    public static final int H = 500;


    private LogPane log = new LogPane();

    public GitShareFrame() throws HeadlessException {
        super("GitHub 分享工具");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //禁止调整大小
        setResizable(false);
        setSize(W, H);
        // 居中
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screenSize.width - getWidth()) / 2;
        int y = (screenSize.height - getHeight()) / 2;
        setLocation(x, y);
        //绝对布局
        setLayout(null);
        addContainer();
        //必须最后调用
        setVisible(true);
    }

    /**
     * 添加内容
     */
    private void addContainer() {
        addTop();
        addList();
        addRightController();
        addBottomLog();
    }


    /**
     * 顶部
     */
    private void addTop() {
        JLabel label = new JLabel("当前路径: ");
        label.setBounds(PADDING, PADDING, 60, 20);
        label.setOpaque(true);

        JTextField jTextField = new JTextField();
        jTextField.setBounds(80, PADDING, W - 200, 20);
        jTextField.setText("C:\\Users\\zll88\\IdeaProjects\\GithubShareUtil\\src\\main\\java\\org\\lulu\\share\\FileListDemo.java");
        add(label);
        add(jTextField);
    }

    /**
     * 文件列表
     */
    private void addList() {
        JPanel jPanel = new JPanel();
        jPanel.setBounds(PADDING, 40, W - 130, H - 200);
        jPanel.setLayout(new BorderLayout(2, 2));
        jPanel.setBackground(Color.WHITE);
        GitFileFrame gitFileFrame = new GitFileFrame();
        JScrollPane comp = new JScrollPane(gitFileFrame.getFileList());
        jPanel.add(comp);
        add(jPanel);
    }

    /**
     * 右侧控制区
     */
    private void addRightController() {
        int w = 110;
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new FlowLayout());
        jPanel.setBounds(W - w, PADDING, w - 20, H - 60);
//        jPanel.setBackground(Color.RED);
        Box box = Box.createVerticalBox();


        JButton clear = new JButton("清除日志");
        clear.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                log.clear();
            }
        });
        box.add(clear);
        box.add(Box.createVerticalStrut(10));
        JButton test = new JButton("测-----试");
        test.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                log.log("测试:" + System.currentTimeMillis());
            }
        });
        box.add(test);
        jPanel.add(box);
        add(jPanel);
    }

    /**
     * 底部日志区
     */
    private void addBottomLog() {
        int h = 100;
        log.setBounds(PADDING, H - h - 50, W - 130, h);
        add(log);
    }

}
