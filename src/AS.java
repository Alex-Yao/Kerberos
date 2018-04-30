import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class AS extends JFrame {
    public PrintWriter writer;
    public BufferedReader reader;
    public ServerSocket server;
    private Socket socket;
    static String ADc = "CUG";
    String IDv = "123433";
    public String K_ctgs;
    byte[] Kc = {1,2,3,4,5,6,7,1};
    byte[] Ktgs = {1,2,3,4,5,6,7,4};


    JTextArea ta = new JTextArea();// 文本域
    Container cc;

    public AS(String title) {
        super(title);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        cc=this.getContentPane();
        final JScrollPane scrollPane = new JScrollPane();
        scrollPane.setBorder(new BevelBorder(BevelBorder.RAISED));
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        scrollPane.setViewportView(ta);

        JPanel panel=new JPanel();
        panel.setLayout(null);
        JButton bt1= new JButton("Start Server");
        JButton bt2= new JButton("Stop Server");
        ta.setBounds(20,20,540,200);
        bt1.setBounds(150,220,120,30);
        bt2.setBounds(320,220,120,30);
        panel.add(ta);
        panel.add(bt1);
        panel.add(bt2);
        cc.add(panel);

        bt2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

    }

    public void getserver() {
        try {
            server = new ServerSocket(4546);
            ta.append("AS服务器套接字已经创建成功\n");
            ta.append("等待客户机的连接\n");
            int i = 0;
            //noinspection Duplicates
            while (true) {
                socket = server.accept();
                i++;
                Client c = new Client(i, socket);
                Thread t = new Thread(c); // 创建客户端处理线程
                t.start(); // 启动线程

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getClientMessage() {
        try {
            String str = reader.readLine(); // 输出
            ta.append("客户机:" + str + '\n');

            String str1 = str.substring(0, 8); // 首部 1
            String IDc = str.substring(8, 14); // IDc
            String IDtgs = str.substring(14, 20);// IDtgs
            //String str4 = str.substring(20, 33);// TS1
            //String str5 = str.substring(33,35);//Lifetime2

            if (str1.equals("00010000")) {
                ta.append("收到package p1 \n");

                long TS2 = getTimeStamp();
                String Lifetime2="10";
                K_ctgs=IDc.substring(0,4)+IDtgs.substring(2,6);
                String str11 = K_ctgs + IDc + ADc + IDtgs + TS2 + Lifetime2; //K_c_tgs + Lifetime2
                String Ticket1 = DES.encrypt(str11, Ktgs);  // Ticket_tgs
                ta.append("Ticket1内容:"+Ticket1+"\n");
                String p = "00100000" + K_ctgs + IDtgs + TS2 + Ticket1 + Lifetime2;
                ta.append("AS返回包内容:" + p + "\n");

                String p2 = DES.encrypt(p, Kc);//Kc encrypt
                writer.println(p2);
                ta.append("经Kc加密:" + p2 + "\n");// 发送package2
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        //noinspection Duplicates
        try {
            if (reader != null) {
                reader.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final class Client implements Runnable { // 多线程
        int clientIndex = 0; // 保存客户端id
        Socket s = null; // 保存客户端Socket对象

        Client(int i, Socket socket) {
            clientIndex = i;
            this.s = socket;
        }

        public void run() {

            //noinspection Duplicates
            try {
                ta.append("客户端连接成功：\n");
                reader = new BufferedReader(new InputStreamReader(socket
                        .getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true);
                getClientMessage();
            } catch (Exception e) {
                System.out.println("客户端断开连接！");
            }
        }
    }

    public long getTimeStamp() // 时间戳
    {
        long now = System.currentTimeMillis();
        return now;
    }

    public static void main(String[] args) {
        AS tcp = new AS("服务器");
        tcp.setSize(600, 300);
        tcp.setVisible(true);
        tcp.setLocationRelativeTo(null);
        tcp.getserver();
        tcp.getClientMessage();


    }

}


