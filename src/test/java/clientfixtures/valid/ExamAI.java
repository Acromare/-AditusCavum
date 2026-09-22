package clientfixtures.valid;

import com.heng.aditus.AditusOperations;
import com.heng.aditus.annotation.AditusClient;

@AditusClient
public interface ExamAI extends AditusOperations {
    default String ask(String message) { return chat("exam", message); }
}
