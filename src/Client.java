
import org.apache.commons.codec.digest.DigestUtils;
import sun.misc.CRC16;

import java.io.*;
import java.net.Socket;
import java.util.Random;
import java.util.zip.CRC32;

public class Client {

    boolean closeFlag = false;

    String salt;
    String username;
    String password;


    int N = 1097; // Честно признаюсь взял у одногруппника значения N и G, но лабу делал сам, I swear
    int g = 11;
    int k = 3; // Const
    int v; // v = g^x mod N
    int A, B;
    int a;
    int K;
    int R;

    int M;

    int S;

    int X, U;


    public static void main(String[] args) throws IOException {
        Socket server = new Socket("localhost", 8080);
        Client main = new Client();
        try{
            System.out.println("Socket = " + server);

            DataInputStream input = new DataInputStream(server.getInputStream());
            DataOutputStream out = new DataOutputStream(server.getOutputStream());

            BufferedReader consoleIn = new BufferedReader(new InputStreamReader(System.in));

            System.out.println("Phase 1 (Registration)");
            System.out.println("//////////////////////////////");

            System.out.print("Enter a username: ");
            main.username = consoleIn.readLine();

            // Работа с солью и паролем
            System.out.print("Enter a password: ");
            main.password = consoleIn.readLine();
            main.salt = main.saltGeneration();
            main.X = main.hash(main.salt + main.password);

            // Поиск верификатора
            main.setV(main.g, main.X, main.N);

            // Передача данных серверу
            out.writeUTF(""); // Костыль для ожидания ответа со стороны сервера
            out.writeUTF(main.username);
            out.flush();
            out.writeUTF(main.salt);
            out.flush();
            out.writeInt(main.v);
            System.out.println("Client sended: " +
                    "\nUsername: " + main.username +
                    "\nSalt: " + main.salt +
                    "\nVerificator: " + main.v);
            System.out.println("X = " + main.X);
            System.out.println("Registration ended...");
            System.out.println("//////////////////////////////\n");

            System.out.println("Phase 2 (Authentication)");
            System.out.println("//////////////////////////////");

            System.out.print("Enter a username: ");
            main.checkUserName(out, consoleIn);

            System.out.print("Enter a password: ");
            main.password = consoleIn.readLine();

            main.setA(main.N, main.g);
            System.out.println("A = " + main.A);
            out.writeInt(main.A);
            out.flush();

            main.B = input.readInt();
            main.checkB(main.B);

            main.U = main.hash("" + main.A + main.B);
            if (main.U == 0){
                System.out.println("U == 0... Sorry, but I must close connection");
                server.close();
            }
            System.out.println("U = " + main.U);

            main.X = main.hash(main.salt + main.password);

            main.S = main.setS(main.B, main.k, main.g, main.X, main.N, main.a, main.U);
            System.out.println("S = " + main.S);

            main.K = main.hash(""+main.S);
            System.out.println("K = " + main.K);

            System.out.println("Authentication ended...");
            System.out.println("//////////////////////////////\n");

            System.out.println("Phase 3 (Generate Confirmation)");
            System.out.println("//////////////////////////////");

            main.M = main.setM(main.S, main.A, main.B, main.K);
            out.writeInt(main.M);
            out.flush();

            System.out.println("M = " + main.M);

            main.R = main.hash("" + main.A + main.M + main.K);
            out.writeInt(main.R);
            out.flush();

            System.out.println("R = " + main.R);
        }catch (IOException e){
            e.printStackTrace();
        }
        finally {
            System.out.println("closing...");
            server.close();
        }
    }

    private void checkUserName(DataOutputStream out, BufferedReader consoleIn) throws IOException {
        String str = consoleIn.readLine();
        out.writeUTF(str);
        out.flush();
        while (!str.equals(this.username)){
            System.out.print("Authentication failed, please enter right username: ");
            str = consoleIn.readLine();
            out.writeUTF(str);
            out.flush();
        }
    }

    private void checkPassword(BufferedReader consoleIn) throws IOException {
        String str = consoleIn.readLine();

        while (!str.equals(this.password)){
            System.out.print("Authentication failed, please enter right password: ");
            str = consoleIn.readLine();

        }
    }

    String saltGeneration(){
        int[] charCodes = new int[36];
        char c = '0';
        Random random = new Random();
        for (int i = 0; i < 10; i++){
            charCodes[i] = (int) c;
            c++;
        }
        c = 'a';
        for (int i = 10; i < 36; i++){
            charCodes[i] = (int) c;
            c++;
        }
        String salt = "";
        for (int i = 0; i < 16; i++){
            salt += (char) charCodes[random.nextInt(35)];
        }
        return salt;
    }

    int hash (String data){
        byte[] sha = DigestUtils.sha(data);
        long hash;
        CRC32 crc32 = new CRC32();
        crc32.update(sha);
        hash = crc32.getValue();
        hash %= 1000; // Сократим время обработки, а то очень долго в степень возводит
        return (int) hash;
    }

    void setV(int g, int x, int N){
        int rest = 1;
        for (int i = 0; i < x; i++){
            rest *= g;
            rest %= N;
        }
        this.v = rest;
    }

    void setA(int N, int g) throws IOException{
        Random random = new Random();
        this.a = random.nextInt(255); // 1-байтное значение
        int rest = 1;
        for (int i = 0; i < a; i++){
            rest *= g;
            rest %= N;
        }
        if (rest != 0){
            this.A = rest;
        } else{
            System.out.println("A == 0");
            throw new IOException("A == 0");
        }
    }

    private void checkB(int b) throws IOException {
        if (b == 0){
            System.out.println("B == 0");
            throw new IOException("B == 0");
        } else {
            System.out.println("B = " + b);
        }
    }

    int setS(int B, int k, int g, int x, int N, int a, int U){
        int tmpGXmodN = 1;
        for (int i = 0; i < x; i++){
            tmpGXmodN *= g;
            tmpGXmodN %= N;
        }
        int tmpBK = B - k * tmpGXmodN;
        int tmpAUX = a + U * x;
        int S = 1;
        for (int i = 0; i < tmpAUX; i++){
            S *= tmpBK;
            S %= N;
        }
        return S;
    }

    // Как же я задолбался...........
    int setM(int S, int A, int B, int K){
        int tmpHashN = hash("" + this.M);
        int tmpHashG = hash("" + this.g);
        int tmpHashI = hash(this.username);
        int tmpXOR = tmpHashN ^ tmpHashG;
        int M = hash("" + tmpXOR + tmpHashI + S + A + B + K);
        return M;
    }
}
