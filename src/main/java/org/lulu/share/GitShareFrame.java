package org.lulu.share;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;

public class GitShareFrame extends JFrame {
    public static final String DEFAULT_BRANCH = "master";

    public static final int PADDING = 10;

    public static final String KEY_UN = "un";

    public static final String KEY_PWD = "pwd";

    public static final int W = 500;

    public static final int H = 500;

    private final LogPanel log = new LogPanel();

    private final FileList fileList = new FileList();

    private final JComboBox<String> branchListBox = new JComboBox<>();

    /**
     * 当前 Git 路径
     */
    private String curGitPath;

    private GitHelper gitHelper;

    private CredentialsProvider credentialsProvider;
    private JLabel gitPathLabel;

    private JCheckBoxSet checkBoxSet = new JCheckBoxSet();
    private volatile boolean isReleasing = false;
    private ActionListener branchListBoxListener;

    public GitShareFrame() throws HeadlessException {
        super("GitHub 分享工具");
        KVStorage.init();
        fileList.setShowHide(false);
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
        createGitHelper(KVStorage.get("git_path", ""));

        //必须最后调用
        setVisible(true);
    }

    ///////////////////////////////////////////////////////////////////////////
    // 基础逻辑控制
    ///////////////////////////////////////////////////////////////////////////

    private void configCredentialProvider() {
        String un = KVStorage.get(KEY_UN, "");
        String pwd = KVStorage.get(KEY_PWD, "");
        if (StringUtils.isAnyEmpty(un, pwd)) {
            log.e("未配置 GitHub 令牌 =>【令牌配置】");
            return;
        }
        log.i("令牌配置成功^v^");
        credentialsProvider = new UsernamePasswordCredentialsProvider(un, pwd);

        if (gitHelper != null) {
            gitHelper.setProvider(credentialsProvider);
        }
    }

    private void createGitHelper(String gitPath) {
        if (StringUtils.isEmpty(gitPath)) {
            log.e("未配置仓库 =>拖拽或【仓库配置】");
            return;
        }
        try {
            gitHelper = new GitHelper(new File(gitPath));
            configCredentialProvider();
            curGitPath = gitPath;
            KVStorage.put("git_path", curGitPath);
            log.i("仓库地址已配置: " + gitPath);
        } catch (IOException e) {
            log.e("请确保该目录为 Git 工程!");
            gitHelper = null;
            curGitPath = "";
        }

        refreshCurGitPath();
        TaskHandler.getInstance().enqueue(this::refreshBranchList);
        if (!StringUtils.isEmpty(curGitPath)) {
            fileList.openItem(new File(curGitPath));
        }
    }

    private void copyShareLink(File file) {
        if (isReleasing) {
            return;
        }
        if (gitHelper == null) {
            return;
        }
        if (file.isDirectory()) {
            log.e("请选择文件!");
            return;
        }
        String remoteUrl = gitHelper.getRemoteUrl();
        if (StringUtils.isEmpty(remoteUrl)) {
            log.e("未关联远程仓库!请先关联");
            return;
        }
        String filePath = file.getAbsolutePath();
        if (!filePath.startsWith(curGitPath)) {
            log.e("请在选中的 Git 仓库内进行!");
            return;
        }
        //tryAutoRelease();
        if (checkNeedRelease()) {
            log.e("暂未发布 => 【一键发布】");
            return;
        }
        //查找相对路径!
        ArrayList<String> pathList = new ArrayList<>();
        File tFile = file;
        do {
            pathList.add(0, tFile.getName());
            tFile = tFile.getParentFile();
        } while (!StringUtils.equals(tFile.getAbsolutePath(), curGitPath));

        StringBuilder relativePath = new StringBuilder();
        for (String s : pathList) {
            try {
                relativePath.append("/").append(URLEncoder.encode(s, "UTF-8").replace("+", "%20"));
            } catch (UnsupportedEncodingException e) {
                log.e("编码失败: " + e.getMessage());
            }
        }

        String curBranch = EncoderUtil.encode(getCurBranch());
        String result;
        if (file.getName().endsWith("html")) {
            String htmlPreviewPrefix = "https://htmlpreview.github.io/?";
            result = htmlPreviewPrefix + remoteUrl + "/blob/" + curBranch + relativePath;
        } else {
            result = "https://raw.githubusercontent.com" + remoteUrl.replace("https://github.com", "") + "/" + curBranch + relativePath;
        }
        //log.i("相对路径:" + t);
        ClipUtil.setSysClipboardText(result);
        log.i("成功复制到剪切板: " + result);
    }

    private void copyFileToHere(File selectedFile) {
        log.i("正在 copy " + selectedFile);
        try {
            File nowDir = fileList.getNowDir();
            File[] files = nowDir.listFiles();
            if (files == null) {
                return;
            }
            boolean hasSameNameFile = false;
            for (File file : files) {
                if (StringUtils.equals(file.getName(), selectedFile.getName())) {
                    hasSameNameFile = true;
                    break;
                }
            }
            if (hasSameNameFile) {
                int i = JOptionPane.showConfirmDialog(GitShareFrame.this, "包含同名文件, 是否覆盖?", "警告!", JOptionPane.YES_OPTION, JOptionPane.WARNING_MESSAGE);
                if (i != 0) {
                    log.i("copy 取消!");
                    return;
                }
            }
            FileUtils.copyFile(selectedFile, new File(nowDir, selectedFile.getName()));
            fileList.refresh();
        } catch (Exception e) {
            log.e("copy 出错!");
        }
        log.i("copy 完成 ^v^");
    }
    private void asyncRelease() {
        TaskHandler.getInstance().enqueue(this::syncRelease);
    }

    private void syncRelease() {
        try {
            if (isReleasing) {
                return;
            }
            if (gitHelper == null) {
                createGitHelper(curGitPath);
                return;
            }
            isReleasing = true;
            gitHelper.createBranchWithRemote(getCurBranch());
            log.i("开始发布");
            log.i("开始拉取...");
            gitHelper.pull();
            log.i("开始提交...");
            gitHelper.commit("commit: " + System.currentTimeMillis());
            log.i("开始推送...");
            gitHelper.push();
            log.i("发布完成");
        } catch (Exception e) {
            log.e("git 发生错误: 请检查配置" + e.getMessage());
        }
        isReleasing = false;
    }

    private boolean checkNeedRelease() {
        Repository repository = gitHelper.getRepository();
        String curBranch = getCurBranch();
        try {
            // 1. 检查是否提交
            if (!repository.resolve("origin/" + curBranch).equals(repository.resolve(curBranch))) {
                return true;
            }
            //2. 检查是否有未 add commit 的
            if (!gitHelper.status().isClean()) {
                return true;
            }
        } catch (IOException | GitAPIException e) {
            log.e("git 发生错误: " + e.getMessage());
        }
        return false;
    }

    public String getCurBranch() {
        return KVStorage.get("cur_branch", DEFAULT_BRANCH);
    }

    public void setCurBranch(String branch) {
        KVStorage.put("cur_branch", branch);
    }

    private void workThreadSwitchBranch(String selectedItem, Runnable callback) {
        TaskHandler.getInstance().enqueue(() -> {
            log.i("备份当前空间");
            syncRelease();
            log.i("清空当前空间");
            clearCruWorkTree();
            log.i("切换" + selectedItem + "分支...");
            setCurBranch(selectedItem);
            try {
                gitHelper.createBranchWithRemote(selectedItem);
                fileList.refresh();
                log.i(selectedItem + "分支切换成功^v^");
                if (callback != null) {
                    callback.run();
                }
            } catch (GitAPIException ex) {
                log.e(selectedItem + "分支切换失败: " + ex.getMessage());
            }
        });
    }

    private void clearCruWorkTree() {
        File workTree = gitHelper.repository.getWorkTree();
        File[] files = workTree.listFiles();
        if (files != null) {
            for (File file : files) {
                if (StringUtils.equals(".git", file.getName())) {
                    continue;
                }
                FileUtils.deleteQuietly(file);
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // UI 控制
    ///////////////////////////////////////////////////////////////////////////

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
        JButton back = new JButton("返回");
        back.setBounds(PADDING, PADDING, 60, 20);
        back.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                fileList.returnDir();
            }
        });
        gitPathLabel = new JLabel();
        gitPathLabel.setBounds(80, PADDING, W - 100, 20);
        add(back);
        add(gitPathLabel);

        addRemoteBranchList();
    }

    private void refreshBranchList() {
        try {
            if (gitHelper == null) {
                addEmptyBranch();
                return;
            }
            String[] remoteList = gitHelper.getRemoteBranch().toArray(new String[0]);
            if (remoteList.length <= 0) {
                addEmptyBranch();
                return;
            }
            Arrays.sort(remoteList);
            DefaultComboBoxModel<String> modelList = new DefaultComboBoxModel<>(remoteList);
            ((DefaultComboBoxModel<String>) branchListBox.getModel()).removeAllElements();
            branchListBox.setModel(modelList);
            branchListBox.setSelectedItem(getCurBranch());

            branchListBox.removeActionListener(branchListBoxListener);
            branchListBoxListener = e -> {
                if (e.getModifiers() <= 0) {
                    return;
                }
                String selectedItem = (String) modelList.getSelectedItem();
                workThreadSwitchBranch(selectedItem, null);
            };
            branchListBox.addActionListener(branchListBoxListener);

        } catch (GitAPIException e) {
            addEmptyBranch();
            log.e("获取远程分支失败!");
        }

    }

    private void addEmptyBranch() {
        DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) branchListBox.getModel();
        model.removeAllElements();
        branchListBox.setModel(new DefaultComboBoxModel<>(new String[]{"加载中.."}));
    }

    private void addRemoteBranchList() {
        JLabel comp = new JLabel("分支:");
        comp.setBounds(W - 180, PADDING, 30, 20);
        add(comp);

        branchListBox.setBounds(comp.getX() + comp.getWidth() + 5, PADDING, 80, 20);
        add(branchListBox);
        String curBranch = getCurBranch();
        //提前保存一份
        setCurBranch(curBranch);
        addEmptyBranch();
        JButton addBranch = new JButton("+");
        addBranch.setMargin(new Insets(0, 0, 0, 0));
        addBranch.setBounds(branchListBox.getX() + branchListBox.getWidth() + 5, (int) (branchListBox.getY() + (branchListBox.getHeight() - 15)/2f), 15, 15);
        addBranch.addActionListener(e -> showAddBranchDialog());
        add(addBranch);

        JButton delBranch = new JButton("-");
        delBranch.setMargin(new Insets(0, 0, 0, 0));
        delBranch.setBounds(addBranch.getX() + addBranch.getWidth() + 5, addBranch.getY(), 15, 15);
        delBranch.addActionListener(e -> {
            String selectedItem = (String) branchListBox.getSelectedItem();
            int i = JOptionPane.showConfirmDialog(GitShareFrame.this, "确认删除分支: "+selectedItem+"?","删除分支", JOptionPane.YES_OPTION, JOptionPane.WARNING_MESSAGE);
            //log.i("i " + i);
            if (i == 0) {
                delBranch(selectedItem);
            }

        });
        add(delBranch);
    }

    private void delBranch(String branch) {

        if (StringUtils.equals(DEFAULT_BRANCH, branch)) {
            log.e("默认分支不可删除!");
            return;
        }
        //删除
        TaskHandler.getInstance().enqueue(() -> {
            try {
                setCurBranch(DEFAULT_BRANCH);
                gitHelper.createBranchWithRemote(DEFAULT_BRANCH);
                log.i("开始删除: " + branch);
                gitHelper.removeBranch(branch);
                log.i("切换默认分支:" + DEFAULT_BRANCH);
                log.i("删除成功^v^");
                refreshBranchList();
                fileList.refresh();
            } catch (GitAPIException ex) {
                log.e("分支删除出错: " + ex.getMessage());
            }
        });
    }

    /**
     * 展示添加分支的 Dialog
     */
    private void showAddBranchDialog() {
        String newBranch = JOptionPane.showInputDialog(this, "请输入分支名: ", "添加分支", JOptionPane.QUESTION_MESSAGE);
        if (StringUtils.isEmpty(newBranch)) {
            return;
        }
        log.i("正在创建分支: " + newBranch);
        workThreadSwitchBranch(newBranch, this::refreshBranchList);
    }

    private void refreshCurGitPath() {
        gitPathLabel.setText("当前仓库地址: " + curGitPath);
    }

    /**
     * 文件列表
     */
    private void addList() {
        JPanel jPanel = new JPanel();
        jPanel.setTransferHandler(SelectFileUtil.createDragTransferHandler(selectedFile -> {
            if (gitHelper == null) {
                createGitHelper(selectedFile.getAbsolutePath());
            } else {
                copyFileToHere(selectedFile);
            }
        }));

        JLabel jTextField = new JLabel();
        jTextField.setBounds(80, PADDING, W - 200, 20);

        fileList.setFileOpenListener(nowDir -> {
            jTextField.setText(nowDir.getAbsolutePath());
        });

        fileList.setFileRightSelectedListener((e, file) -> {
            JPopupMenu jPopupMenu = new JPopupMenu();
            JMenuItem copyLink = new JMenuItem("复制分享链接");
            copyLink.addActionListener(e1 -> copyShareLink(file));
            JMenuItem delete = new JMenuItem("删除");
            delete.addActionListener(e12 -> {
                try {
                    File dir = file.getParentFile();
                    FileUtils.deleteQuietly(file);
                    fileList.openItem(dir);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            JMenuItem rename = new JMenuItem("重命名");
            rename.addActionListener(e1 -> {
                String newName = (String) JOptionPane.showInputDialog(this, "请输入文件名: ", "重命名", JOptionPane.QUESTION_MESSAGE, null, null, file.getName());
                if (StringUtils.isEmpty(newName)) {
                    return;
                }
                if (file.renameTo(new File(file.getParentFile(), newName))) {
                    log.i("重命名成功: " + newName);
                    fileList.refresh();
                } else {
                    log.e("重命名失败");
                }
            });
            jPopupMenu.add(copyLink);
            jPopupMenu.add(rename);
            jPopupMenu.add(delete);
            jPopupMenu.show(fileList, e.getX(), e.getY());
        });

        jPanel.setBounds(PADDING, 40, W - 130, H - 200);
        jPanel.setLayout(new BorderLayout(2, 5));
        jPanel.setBackground(Color.WHITE);
        JScrollPane comp = new JScrollPane(fileList);

        jPanel.add(jTextField, BorderLayout.NORTH);
        jPanel.add(comp);
        JTextField searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(0, 25));
        JTextFieldHintListener hintListener = new JTextFieldHintListener(searchField, "在此输入开始搜索文件");
        searchField.addFocusListener(hintListener);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                trySearch(e);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                trySearch(e);

            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
        jPanel.add(searchField, BorderLayout.SOUTH);

        add(jPanel);
    }

    private void trySearch(DocumentEvent e) {
        Document document = e.getDocument();
        try {
            String searchKey = document.getText(0, document.getLength());
            //log.i("SearchKey: " + searchKey);
            if (StringUtils.isEmpty(searchKey) || StringUtils.equals(searchKey, "在此输入开始搜索文件")) {
                fileList.refresh();
            } else {
                fileList.searchFile(searchKey);
            }
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * 右侧控制区
     */
    private void addRightController() {
        int w = 110;
        JPanel jPanel = new JPanel();
        jPanel.setLayout(new FlowLayout());
        jPanel.setBounds(W - w, PADDING + 20, w - 20, H - 60);
//        jPanel.setBackground(Color.RED);
        Box box = Box.createVerticalBox();
        addRightButton(box, "返回仓库", event -> {
            if (StringUtils.isEmpty(curGitPath)) {
                log.e("请配置仓库");
                return;
            }
            fileList.openItem(new File(curGitPath));
        });

        addRightButton(box, "仓库地址", event -> {
            SelectFileUtil.selectDir(curGitPath, selectedFile -> {
                createGitHelper(selectedFile.getAbsolutePath());
            });
        });
        addRightButton(box, "令牌配置", event -> {
            showGitTokenConfigDialog();
        });

        addRightButton(box, "一键发布", event -> {
            asyncRelease();
        });

        addRightButton(box, "刷         新", event -> {
            fileList.refresh();
        });
        addRightButton(box, "清除日志", event -> {
            log.clear();
        });
        addShowHintFileCheck(box);
        jPanel.add(box);
        add(jPanel);
    }

    private void addShowHintFileCheck(Box box) {
        box.add(Box.createVerticalStrut(10));
        boolean showHintFile = StringUtils.equals(KVStorage.get("show_hint_file", "FALSE"), "TRUE");
        fileList.setShowHide(showHintFile);
        checkBoxSet.showHintFile.setSelected(showHintFile);
        checkBoxSet.showHintFile.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                KVStorage.put("show_hint_file", checkBoxSet.showHintFile.isSelected() ? "TRUE" : "FALSE");
                fileList.setShowHide(checkBoxSet.showHintFile.isSelected());
                fileList.refresh();
            }
        });
        box.add(checkBoxSet.showHintFile);
    }

    private void showGitTokenConfigDialog() {
        JDialog dialog = new JDialog(this, "令牌配置", true);
        dialog.setSize(202, 180);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screenSize.width - dialog.getWidth()) / 2;
        int y = (screenSize.height - dialog.getHeight()) / 2;
        dialog.setLocation(x, y);
        dialog.setDefaultCloseOperation(dialog.HIDE_ON_CLOSE);

        Container pane = dialog.getContentPane();
        pane.setLayout(null);

        JLabel label0 = new JLabel("GitHub 访问令牌: ");
        label0.setBounds(PADDING, PADDING, 100, 25);
        pane.add(label0);
        JLabel label1 = new JLabel("用户名:");
        label1.setBounds(PADDING, PADDING + 30, 50, 25);
        pane.add(label1);
        JTextField unField = new JTextField();
        unField.setBounds(60, PADDING + 30, 80, 25);
        pane.add(unField);

        JLabel label2 = new JLabel("令牌:");
        label2.setBounds(PADDING, PADDING + 60, 50, 25);
        pane.add(label2);
        JPasswordField pwdField = new JPasswordField();
        pwdField.setBounds(60, PADDING + 60, 80, 25);
        pane.add(pwdField);

        String un = KVStorage.get(KEY_UN, "");
        String pwd = KVStorage.get(KEY_PWD, "");
        unField.setText(un);
        pwdField.setText(pwd);

        JButton gitPathButton = new JButton("保存");
        gitPathButton.setBounds(PADDING + 10, PADDING + 90, 60, 30);
        gitPathButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                KVStorage.put(KEY_UN, unField.getText());
                KVStorage.put(KEY_PWD, pwdField.getText());
                //log.i("用户名密码已保存");
                dialog.dispose();
                configCredentialProvider();
            }
        });
        pane.add(gitPathButton);

        dialog.setVisible(true);
    }

    private interface OnClickListener {
        void onClick(MouseEvent event);
    }

    private void addRightButton(Box box, String text, OnClickListener listener) {
        box.add(Box.createVerticalStrut(10));
        JButton button = new JButton(text);
        //居中
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                listener.onClick(e);
            }
        });
        box.add(button);
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
