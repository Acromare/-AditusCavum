# AditusCavum

Lightweight Java model and tool-calling foundation.

## Maven dependency

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.Acromare</groupId>
    <artifactId>-AditusCavum</artifactId>
    <version>v0.1.0</version>
</dependency>
```

The starter auto-configures `AditusAssistant`, the model client, tool registry,
and in-memory conversation memory. Consumers do not need to scan the AditusCavum
package explicitly.

## Minimal tool

```java
@MarkTheRuins
@Component
public class WeatherTools {

    @Need("查询指定城市的天气")
    public Weather queryWeather(String city) {
        return weatherService.query(city);
    }
}
```

Call the model from an injected `AditusAssistant`:

```java
String answer = assistant.chat("查询上海今天的天气");
assistant.stream("查询上海今天的天气").subscribe(System.out::print);

For a conversation with memory, pass a stable conversation id:

```java
assistant.chat("user-1001", "我叫张三");
assistant.chat("user-1001", "我叫什么？");
assistant.clearConversation("user-1001");
```
```

The framework scans marked beans, builds an OpenAI-compatible function schema from
the method signature, executes model-selected tools, and sends tool results back
to the model. Business methods remain application code; AditusCavum does not
generate SQL or database implementations.

Configure any OpenAI-compatible endpoint with environment variables:

```text
ADITUS_MODEL_BASE_URL=https://api.deepseek.com/v1
ADITUS_MODEL_API_KEY=your-key
ADITUS_MODEL_NAME=deepseek-chat
```
