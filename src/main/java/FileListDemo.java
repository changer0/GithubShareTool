import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.filechooser.FileSystemView;

final class FileListDemo {
    private static final FileSystemView fsv = FileSystemView.getFileSystemView();

    public static void main(String[] args) {

        // 窗口创建
        JFrame f = new JFrame("文件列表浏览器");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(300, 500);
        // 居中
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screenSize.width - f.getWidth()) / 2;
        int y = (screenSize.height - f.getHeight()) / 2;
        f.setLocation(x, y);

        // 容器
        Container root = f.getContentPane();
        FileList fileList = new FileList();
        root.setLayout(new BorderLayout(2, 2));
        // 顶部
        {
            Box top = Box.createHorizontalBox();
            // 容器
            top.setOpaque(false);
            top.setPreferredSize(new Dimension(top.getWidth(), 30));

            // 控件
            JButton returnButton = new JButton("←");
            returnButton.setToolTipText("返回");
            returnButton.setPreferredSize(new Dimension(45, top.getHeight()));
            returnButton.addActionListener((e) -> fileList.returnDir());
            JTextField filePath = new JTextField();
            fileList.setFileOpenListener((e) -> {
                if (e != null)
                    filePath.setText(e.getAbsolutePath());
            });
            filePath.addActionListener((e) -> {
                File file = new File(filePath.getText());
                if (file.exists())
                    fileList.openItem(new FileItem(fsv.getSystemDisplayName(file), fsv.getSystemIcon(file), file));
                else
                    filePath.setText("");
            });

            // 添加
            top.add(returnButton);
            top.add(filePath);
            root.add(top, BorderLayout.PAGE_START);
        }
        // 中间
        root.add(new JScrollPane(fileList));

        f.setVisible(true);
    }

    // 文件打开监听器
    private interface FileOpenListener {
        void fileOpenEvent(File nowDir);
    }

    private static class FileList extends JList<FileItem> {
        private File nowDir;
        private FileOpenListener fileOpenListener;

        public FileList() {
            setCellRenderer(new FileListCellRenderer());
            setModel(new FileListModel());

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    super.mouseClicked(e);

                    if (e.getClickCount() == 2) {
                        int index = locationToIndex(e.getPoint());
                        if (index >= 0) {
                            FileItem v = getModel().getElementAt(index);
                            if (v.file.isDirectory())
                                openItem(v);
                        }
                    }
                }
            });
        }

        private void openItem(FileItem item) {
            File[] subFiles;
            if (item.file != null)
                subFiles = fsv.getFiles(item.file, false);
            else
                subFiles = fsv.getRoots();

            ((DefaultListModel<FileItem>) getModel()).clear();
            for (File subFile : subFiles)
                ((DefaultListModel<FileItem>) getModel()).addElement(new FileItem(fsv.getSystemDisplayName(subFile),
                        fsv.getSystemIcon(subFile), subFile));

            if (item.file != null)
                nowDir = item.file;

            // 回调监听器
            if (fileOpenListener != null)
                fileOpenListener.fileOpenEvent(nowDir);
        }

        private void returnDir() {
            if (nowDir == null)
                return;

            nowDir = fsv.getParentDirectory(nowDir);
            openItem(new FileItem(fsv.getSystemDisplayName(nowDir), fsv.getSystemIcon(nowDir), nowDir));
        }

        public FileOpenListener getFileOpenListener() {
            return fileOpenListener;
        }

        public void setFileOpenListener(FileOpenListener fileOpenListener) {
            this.fileOpenListener = fileOpenListener;
        }
    }

    private static class FileListModel extends DefaultListModel<FileItem> {
        public FileListModel() {
            // 初始化系统根目录
            File[] roots = fsv.getRoots();
            for (File root : roots)
                addElement(new FileItem(fsv.getSystemDisplayName(root), fsv.getSystemIcon(root), root));
        }
    }

    private static class FileListCellRenderer extends JLabel implements ListCellRenderer<FileItem> {
        public FileListCellRenderer() {
            setPreferredSize(new Dimension(getWidth(), 25));
            setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends FileItem> list, FileItem value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            setText(value.name);
            setIcon(value.icon);
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            return this;
        }
    }

    // 文件项
    private static class FileItem {
        public String name;// 文件名
        public Icon icon;// 文件图标
        public File file;// 文件

        public FileItem(String name, Icon icon, File file) {
            this.name = name;
            this.icon = icon;
            this.file = file;
        }
    }
}