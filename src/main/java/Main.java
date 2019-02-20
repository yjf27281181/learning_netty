import server.KidsRequestDispatcher;
import server.internal.HttpServer;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class Main {

    static class Node {
        int num = 0;
        public Node(int num) {
            this.num = num;
        }
    }
    public static void main(String[] args) {
        var rd = new KidsRequestDispatcher("/kids", new Router((ctx, req) -> {
            ctx.html("Hello, World");
        }));
        new HttpServer("localhost", 8080, 2, 16, rd).start();
    }
}
