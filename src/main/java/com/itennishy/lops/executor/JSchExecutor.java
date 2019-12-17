package com.itennishy.lops.executor;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Scanner;
import java.util.Vector;

@Slf4j
public class JSchExecutor {

    private static final int TIME_OUT = 30 * 1000;
    private String charset = "UTF-8"; // 设置编码格式
    private String user; // 用户名
    private String passwd; // 登录密码
    private String host; // 主机IP
    private int port = 22; //默认端口
    private JSch jsch;
    private Session session;

    private Vector<String> stdout;

    private ChannelSftp sftp;

    public JSchExecutor() {
    }

    /**
     * @param user   用户名
     * @param passwd 密码
     * @param host   主机IP
     */
    public JSchExecutor(String user, String passwd, String host) {
        this.user = user;
        this.passwd = passwd;
        this.host = host;

        stdout = new Vector<String>();
    }

    /**
     * @param user   用户名
     * @param passwd 密码
     * @param host   主机IP
     */
    public JSchExecutor(String user, String passwd, String host, int port) {
        this.user = user;
        this.passwd = passwd;
        this.host = host;
        this.port = port;

        stdout = new Vector<String>();
    }

    public Vector<String> getStandardOutput() {
        return stdout;
    }

    /**
     * 连接到指定的IP
     *
     * @throws JSchException
     */
    public void connect() throws JSchException {
        jsch = new JSch();
        session = jsch.getSession(user, host, port);
        session.setPassword(passwd);
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.setTimeout(TIME_OUT);
        session.connect();
        Channel channel = session.openChannel("sftp");
        channel.connect();
        sftp = (ChannelSftp) channel;
        log.info("连接到SFTP成功。host: " + host);
    }

    /**
     * 关闭连接
     */
    public void disconnect() {
        if (sftp != null && sftp.isConnected()) {
            sftp.disconnect();
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    /**
     * 执行一条命令
     */
    public int execCmd(String command) throws Exception {
        log.info("The remote command is:" + command);
        int returnCode = -1;
        BufferedReader reader = null;
        Channel channel = null;

        channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(command);
        channel.setInputStream(null);
        ((ChannelExec) channel).setErrStream(System.err);
        InputStream in = channel.getInputStream();
        reader = new BufferedReader(new InputStreamReader(in));//中文乱码貌似这里不能控制，看连接的服务器的

        channel.connect();
        String buf;
        while ((buf = reader.readLine()) != null) {
            log.info(buf);
            stdout.add(buf);
        }

        reader.close();
        // Get the return code only after the channel is closed.
        if (channel.isClosed()) {
            returnCode = channel.getExitStatus();
        }
        log.info("Exit-status:" + returnCode);

        channel.disconnect();
        return returnCode;
    }

    /**
     * 实时打印日志信息
     */
    public int shellCmd(String command) throws Exception {
        log.info("开始执行命令:" + command);
        int returnCode = -1;
        ChannelShell channel = (ChannelShell) session.openChannel("shell");
        InputStream in = channel.getInputStream();
        channel.setPty(true);
        channel.connect();
        OutputStream os = channel.getOutputStream();
        os.write((command + "\r\n").getBytes());
        os.write("exit\r\n".getBytes());
        os.flush();
        log.info("The remote command is:{}", command);
        byte[] tmp = new byte[1024];
        while (true) {
            while (in.available() > 0) {
                int i = in.read(tmp, 0, 1024);
                if (i < 0) break;
                log.info(new String(tmp, 0, i));
            }
            if (channel.isClosed()) {
                if (in.available() > 0) continue;
                returnCode = channel.getExitStatus();
                log.info("exit-status: " + channel.getExitStatus());
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (Exception ee) {
            }
        }
        os.close();
        in.close();
        channel.disconnect();
        session.disconnect();
        return returnCode;
    }

    /**
     * 执行相关的命令
     */
    public void execCmd() {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        String command = "";
        BufferedReader reader = null;
        Channel channel = null;

        try {
            while ((command = br.readLine()) != null) {
                channel = session.openChannel("exec");
                ((ChannelExec) channel).setCommand(command);
                channel.setInputStream(null);
                ((ChannelExec) channel).setErrStream(System.err);

                channel.connect();
                InputStream in = channel.getInputStream();
                reader = new BufferedReader(new InputStreamReader(in,
                        Charset.forName(charset)));
                String buf = null;
                while ((buf = reader.readLine()) != null) {
                    System.out.println(buf);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSchException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            channel.disconnect();
        }
    }


    public void upLoadFile(String sPath, String dPath) {
        try {
            try {
                sftp.cd(dPath);
//                Scanner scanner = new Scanner(System.in);
//                System.out.println(dPath + ":此目录已存在,文件可能会被覆盖!是否继续y/n?");
//                String next = scanner.next();
//                if (!next.toLowerCase().equals("y")) {
//                    return;
//                }

            } catch (SftpException e) {

                sftp.mkdir(dPath);
                sftp.cd(dPath);

            }
            File file = new File(sPath);
            copyFile(sftp, file, sftp.pwd());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void copyFile(ChannelSftp sftp, File file, String pwd) {

        if (file.isDirectory()) {
            File[] list = file.listFiles();
            try {
                try {
                    String fileName = file.getName();
                    sftp.cd(pwd);
                    log.info("正在创建目录:" + sftp.pwd() + "/" + fileName);
                    sftp.mkdir(fileName);
                    log.info("目录创建成功:" + sftp.pwd() + "/" + fileName);
                } catch (Exception e) {
                }
                pwd = pwd + "/" + file.getName();
                try {

                    sftp.cd(file.getName());
                } catch (SftpException e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            for (int i = 0; i < list.length; i++) {
                copyFile(sftp, list[i], pwd);
            }
        } else {

            try {
                sftp.cd(pwd);

            } catch (SftpException e1) {
                e1.printStackTrace();
            }
            log.info("正在复制文件:" + file.getAbsolutePath());
            InputStream instream = null;
            OutputStream outstream = null;
            try {
                outstream = sftp.put(file.getName());
                instream = new FileInputStream(file);

                byte b[] = new byte[1024];
                int n;
                try {
                    while ((n = instream.read(b)) != -1) {
                        outstream.write(b, 0, n);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } catch (SftpException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    outstream.flush();
                    outstream.close();
                    instream.close();

                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }
    }


    /**
     * 下载文件
     */
    public void downloadDirAndFile(String remote, String local) throws Exception {
        try {
            downloadFile(remote, local);
        } catch (Exception e) {
            Vector<ChannelSftp.LsEntry> fileAndFolderList = sftp.ls(remote);
            for (ChannelSftp.LsEntry item : fileAndFolderList) {
                if (!item.getAttrs().isDir()) {
//                if (!(new File(local + "/" + item.getFilename())).exists() || (item.getAttrs().getMTime() > Long.valueOf(new File(local + "/" + item.getFilename()).lastModified() / (long) 1000).intValue())) {
                    new File(local + "/" + item.getFilename());
                    sftp.get(remote + "/" + item.getFilename(), local + "/" + item.getFilename());
                    log.info("下载成功:" + item.getFilename());
//                }
                } else if (!(".".equals(item.getFilename()) || "..".equals(item.getFilename()))) {
                    new File(local + "/" + item.getFilename()).mkdirs();
                    downloadDirAndFile(remote + "/" + item.getFilename(),
                            local + "/" + item.getFilename());
                }
            }
        }
    }

    /**
     * 下载文件
     */
    public void downloadFile(String remote, String local) throws Exception {
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(new File(local));
            sftp.get(remote, outputStream);
            log.info("下载文件成功:" + remote);
            outputStream.flush();
        } catch (Exception e) {
            throw e;
        } finally {
            if (outputStream != null) {
                outputStream.close();
            }
        }
    }

    /**
     * 移动到相应的目录下
     *
     * @param pathName 要移动的目录
     * @return
     */
    public boolean changeDir(String pathName) {
        if (pathName == null || pathName.trim().equals("")) {
            log.debug("invalid pathName");
            return false;
        }
        try {
            sftp.cd(pathName.replaceAll("\\\\", "/"));
            log.debug("directory successfully changed,current dir=" + sftp.pwd());
            return true;
        } catch (SftpException e) {
            log.error("failed to change directory", e);
            return false;
        }
    }

    /**
     * 创建一个文件目录，mkdir每次只能创建一个文件目录
     * 或者可以使用命令mkdir -p 来创建多个文件目录
     */
    public void createDir(String createpath) {
        try {
            if (isDirExist(createpath)) {
                sftp.cd(createpath);
                return;
            }
            String pathArry[] = createpath.split("/");
            StringBuffer filePath = new StringBuffer("/");
            for (String path : pathArry) {
                if (path.equals("")) {
                    continue;
                }
                filePath.append(path + "/");
                if (isDirExist(filePath.toString())) {
                    sftp.cd(filePath.toString());
                } else {
                    // 建立目录
                    sftp.mkdir(filePath.toString());
                    // 进入并设置为当前目录
                    sftp.cd(filePath.toString());
                }
            }
            sftp.cd(createpath);
        } catch (SftpException e) {
            throw new RuntimeException("创建路径错误：" + createpath);
        }
    }


    /**
     * 判断目录是否存在
     *
     * @param directory
     * @return
     */
    public boolean isDirExist(String directory) {
        boolean isDirExistFlag = false;
        try {
            SftpATTRS sftpATTRS = sftp.lstat(directory);
            isDirExistFlag = true;
            return sftpATTRS.isDir();
        } catch (Exception e) {
            if (e.getMessage().toLowerCase().equals("no such file")) {
                isDirExistFlag = false;
            }
        }
        return isDirExistFlag;
    }


    public boolean isLinkExist(String link) {
        boolean isLinkExistFlag = false;
        try {
            sftp.lstat(link);
            isLinkExistFlag = true;
        } catch (Exception e) {
            log.error("Exception Happens:", e);
        }
        return isLinkExistFlag;
    }

}
