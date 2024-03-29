public class Message {
    String content;

    public Message(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    @Printable
    public void printContent(){
        System.out.println("Message: "+content);
    }
}
