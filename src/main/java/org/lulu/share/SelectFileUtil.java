package org.lulu.share;

import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;

public class SelectFileUtil {

    public interface OnSelectFileListener {
        void onSelectedFile(File selectedFile);
    }

    /**
     * 选择文件
     * @param curPath 当前路径
     * @param listener 选择回调
     */
    public static void selectDir(String curPath, OnSelectFileListener listener) {
        //动态获取保存文件路径
        JFileChooser fileChooser = new JFileChooser();
        FileSystemView fsv = FileSystemView.getFileSystemView();     //注意了，这里重要的一句
        //System.out.println(fsv.getHomeDirectory());                  //得到桌面路径
        //设置展示路径
        fileChooser.setCurrentDirectory(new File(
                StringUtils.isEmpty(curPath) ? fsv.getHomeDirectory().getPath() : curPath
        ));
        fileChooser.setDialogTitle("选择目录");
        fileChooser.setApproveButtonText("确定");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return true;
            }

            @Override
            public String getDescription() {
                return ".*";
            }
        });
        int result = fileChooser.showOpenDialog(null);
        if (JFileChooser.APPROVE_OPTION == result) {
            listener.onSelectedFile(fileChooser.getSelectedFile());
            KVStorage.put("mdPath", new File(curPath).getParent());
        }
    }

    public static TransferHandler createDragTransferHandler(OnSelectFileListener listener) {
        return new TransferHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean importData(JComponent comp, Transferable t) {
                try {
                    Object o = t.getTransferData(DataFlavor.javaFileListFlavor);

                    String filepath = o.toString();
                    if (filepath.startsWith("[")) {
                        filepath = filepath.substring(1);
                    }
                    if (filepath.endsWith("]")) {
                        filepath = filepath.substring(0, filepath.length() - 1);
                    }
                    listener.onSelectedFile(new File(filepath));
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }

            @Override
            public boolean canImport(JComponent comp, DataFlavor[] flavors) {
                for (DataFlavor flavor : flavors) {
                    if (DataFlavor.javaFileListFlavor.equals(flavor)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }
}
