package clientfixtures.invalidmethod;

import com.heng.aditus.AditusOperations;
import com.heng.aditus.annotation.AditusClient;

@AditusClient
public interface InvalidAI extends AditusOperations {
    String answerAnything(String question);
}
