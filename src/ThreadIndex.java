import java.io.File;
import java.io.IOException;
import java.util.Iterator;

class ThreadIndex extends Thread{

    private final Iterator<File> filesIter;
    private final InvertedIndex invIndex;

    public ThreadIndex(InvertedIndex invIndex, Iterator<File> filesIter){
        this.filesIter = filesIter;
        this.invIndex = invIndex;
    }

    public void run() {
        while(filesIter.hasNext()){
            try {
                invIndex.indexFile(filesIter.next());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}