import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class InvertedIndex {

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<File>> indexDict;

    InvertedIndex(){
        this.indexDict = new ConcurrentHashMap<String, CopyOnWriteArrayList<File>>();
    }

    List<String> stopWords = Arrays.asList("a", "able", "about",
            "across", "after", "all", "almost", "also", "am", "among", "an",
            "and", "any", "are", "as", "at", "be", "because", "been", "but",
            "by", "can", "cannot", "could", "dear", "did", "do", "does",
            "either", "else", "ever", "every", "for", "from", "get", "got",
            "had", "has", "have", "he", "her", "hers", "him", "his", "how",
            "however", "i", "if", "in", "into", "is", "it", "its", "just",
            "least", "let", "like", "likely", "may", "me", "might", "most",
            "must", "my", "neither", "no", "nor", "not", "of", "off", "often",
            "on", "only", "or", "other", "our", "own", "rather", "said", "say",
            "says", "she", "should", "since", "so", "some", "than", "that",
            "the", "their", "them", "then", "there", "these", "they", "this",
            "tis", "to", "too", "twas", "us", "wants", "was", "we", "were",
            "what", "when", "where", "which", "while", "who", "whom", "why",
            "will", "with", "would", "yet", "you", "your");
    

    public void indexFile(File file) throws IOException{
        int wordsNum = 0;
        BufferedReader reader = new BufferedReader(new FileReader(file));
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            for (String _word : line.split("\\W+")) {
                String word = _word.toLowerCase();
                if (stopWords.contains(word))
                    continue;
                wordsNum++;
                CopyOnWriteArrayList<File> idx = indexDict.putIfAbsent(word, new CopyOnWriteArrayList<File>());
                if (idx != null) {
                    idx.addIfAbsent(file);
                }
            }
        }
        System.out.println("indexed " + file.getPath() + " " + wordsNum + " words");
    }
    

    public void search(List<String> words) {
        for (String _word : words) {
            Set<String> answer = new HashSet<String>();
            String word = _word.toLowerCase();
            List<File> idx = indexDict.get(word);
            if (idx != null) {
                for (File t : idx) {
                    answer.add(t.getName());
                }
            }
            System.out.print(word);
            for (String f : answer) {
                System.out.print(" " + f);
            }
            System.out.println("");
        }
    }

    public void writeToFile(){}

    public static void main(String[] args) {
        int THREADS = 5;
        try {
            FileListReader filesReader = new FileListReader("src/data");
            ArrayList<File> filesList = filesReader.getFiles();
            InvertedIndex invIndex = new InvertedIndex();
            ThreadIndex[] threadArray = new ThreadIndex[THREADS];
            long startTime;
            long finishTime;
            int size = filesList.size();
            for(int i = 0; i < size % THREADS; i++){ //розбиття на потоки
                threadArray[i] = new ThreadIndex(invIndex, filesList.subList( (size/THREADS + 1) * i,
                        (size/THREADS + 1) * (i + 1)).iterator());
            }
            for(int i = size  % THREADS; i < THREADS; i++){ //розбиття на потоки
                threadArray[i] = new ThreadIndex(invIndex, filesList.subList(size/THREADS * i + size % THREADS,
                        size/THREADS * (i + 1) + size % THREADS).iterator());
            }
            startTime = System.nanoTime();
            for(int i = 0; i < THREADS; i++){ //старт потоків
                threadArray[i].start();
            }
            for(int i = 0; i < THREADS; i++){ //очікування завершення усіх потоків
                threadArray[i].join();
            }
            finishTime = System.nanoTime();
            long workTime = (finishTime - startTime) / 1000;
            System.out.println("Indexing by " + THREADS + " threads took " + workTime + "nanoseconds");
            invIndex.search(Arrays.asList("Hello,Great,Bad".split(",")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}