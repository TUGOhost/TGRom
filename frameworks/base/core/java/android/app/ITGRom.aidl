package android.app;
// add
interface ITGRom
{
    String readFile(String path);
    void writeFile(String path,String data);
    String shellExec(String cmd);
    void send(String msg);
    String getSend();
}