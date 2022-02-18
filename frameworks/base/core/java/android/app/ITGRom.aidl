package android.app;
// change tgrom
interface ITGRom
{
    String readFile(String path);
    void writeFile(String path,String data);
    String shellExec(String cmd);
}