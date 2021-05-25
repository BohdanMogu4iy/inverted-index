import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class InvertedIndex {

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<File>> indexDict;

    InvertedIndex(){
        this.indexDict = new ConcurrentHashMap<>();
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
    

    public void indexFile(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            for (String _word : line.split("\\W+")) {
                String word = _word.toLowerCase();
                if (stopWords.contains(word))
                    continue;
                CopyOnWriteArrayList<File> idx = indexDict.putIfAbsent(word, new CopyOnWriteArrayList<>());
                if (idx != null) {
                    idx.addIfAbsent(file);
                }
            }
        }
    }

    public HashSet<String> search(List<String> words) {
        List<HashSet<String>> answer = new ArrayList<>();
        for (String _word : words) {
            String word = _word.toLowerCase();
            if (stopWords.contains(word))
                continue;
            List<File> idx = indexDict.get(word);
            HashSet<String> _answer = new HashSet<>();
            if (idx != null) {
                for (File t : idx) {
                    _answer.add(t.getParentFile() + "/" + t.getName());
                }
            }
            answer.add(_answer);
        }

        return answer.stream().reduce((strings, c) -> {
            strings.retainAll(c);
            return strings;
        }).get();
    }

    public long createIndex(int threads, FileListReader filesReader) throws InterruptedException {
        ThreadIndex[] threadArray = new ThreadIndex[threads];
        ArrayList<File> filesList = filesReader.getFiles();
        int size = filesList.size();
        for(int i = 0; i < size % threads; i++){ //розбиття на потоки
            threadArray[i] = new ThreadIndex(this, filesList.subList( (size/threads + 1) * i,
                    (size/threads + 1) * (i + 1)).iterator());
        }
        for(int i = size  % threads; i < threads; i++){ //розбиття на потоки
            threadArray[i] = new ThreadIndex(this, filesList.subList(size/threads * i + size % threads,
                    size/threads * (i + 1) + size % threads).iterator());
        }
        long startTime;
        long finishTime;
        startTime = System.nanoTime();
        for(int i = 0; i < threads; i++){ //старт потоків
            threadArray[i].start();
        }
        for(int i = 0; i < threads; i++){ //очікування завершення усіх потоків
            threadArray[i].join();
        }
        finishTime = System.nanoTime();
        return (finishTime - startTime);
    }

    public static void main(String[] args) {
        int TESTS = 10;
        int[] THREADS = {1, 2, 3, 4, 5, 6, 7, 8};
        HashMap<Integer, ArrayList<Long>> workTimeList = new HashMap<>();
        try {
            InvertedIndex invIndex = new InvertedIndex();
            FileListReader filesReader = new FileListReader("src/data");
            for (int i =0; i < TESTS; i++){
                for (int threads : THREADS){
                    workTimeList.putIfAbsent(threads, new ArrayList<>());
                    workTimeList.get(threads).add(invIndex.createIndex(threads, filesReader));
                }
            }
            for (int th : workTimeList.keySet()){
                System.out.println("Indexing by " + th + " threads took " + workTimeList.get(th).stream().mapToDouble(a -> a).sum() / TESTS + " nanoseconds");
            }
            Set<String> answer = invIndex.search(Arrays.asList("door".split(",")));
            for (String f : answer) {
                System.out.println(f);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class ThreadIndex extends Thread{

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
}