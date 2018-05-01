package com.evelynkirschner.week_15;


import com.google.gson.Gson;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;

class HttpException extends IOException {
    public HttpException(HttpResponse response) {
        super(response.getStatusLine().getStatusCode() + ": "
                + response.getStatusLine().getReasonPhrase());
    }
}

class HttpRequests {
    private CloseableHttpClient client = HttpClientBuilder.create().build();

    public HttpRequests (String username, String password){
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(username, password));
        client = HttpClientBuilder.create()
                .setDefaultCredentialsProvider(credentialsProvider).build();
    }

    // return true if status is between 200 and 300
    private static boolean isSuccess(HttpResponse response) throws IOException {
        StatusLine status = response.getStatusLine();
        return (status.getStatusCode() >= 200 && status.getStatusCode() < 300);
    }

    // send specified request and return response as a string
    private String makeRequest(HttpRequestBase request) throws IOException {
        CloseableHttpResponse response = client.execute(request);
        try {
            if (!isSuccess(response)) {
                throw new HttpException(response);
            }

            return EntityUtils.toString(response.getEntity());
        }
        finally {
            response.close();
        }
    }

    private void addData(HttpEntityEnclosingRequestBase request, String contentType, String data)
            throws UnsupportedEncodingException {
        request.setHeader("Content-type", contentType);

        StringEntity requestData = new StringEntity(data);
        request.setEntity(requestData);
    }


    // send a GET request to the specified URL
    public String get(String url) throws IOException {
        HttpGet request = new HttpGet(url);
        return makeRequest(request);

    }

    // send a DELETE request to the specified URL
    public String delete(String url) throws IOException {
        HttpDelete request = new HttpDelete(url);
        return makeRequest(request);
    }

    // send a POST request to the specified URL with the specified data
    // and the specified content-type
    public String post(String url, String contentType, String data) throws IOException{
        HttpPost request = new HttpPost(url);
        addData(request, contentType, data);
        return makeRequest(request);
    }

    // send a PUT request to the specified URL with the specified data
    // and the specified content-type
    public String put(String url, String contentType, String data) throws IOException{
        HttpPut request = new HttpPut(url);
        addData(request, contentType, data);
        return makeRequest(request);
    }
}

// a To-do class with a basic constructor, getters, and setters
class Todo {
    private String title;
    private String body;
    private int priority;
    private String uuid = null; //only set an int when the server assigns an ID

    public Todo(String title, String body, int priority) {
        this.title = title;
        this.body = body;
        this.priority = priority;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String toString() {
        return "TODO ID: " + uuid + ", Title: " + title + ", Body: " + body
                + ", Priority: " + priority;
    }
}

// a collection of Todos; only support iteration
class TodoCollection implements Iterable<Todo> {
    private List<Todo> todos;

    public Todo get(Integer index) {
        return todos.get(index);
    }

    @Override
    public Iterator<Todo> iterator() {
        return todos.iterator();
    }
}

class TodoAPIWrapper {
    Gson gson = new Gson();
    private HttpRequests requests;
    private String hostURL;

    TodoAPIWrapper(String username, String password, String hostURL) {
        requests = new HttpRequests(username, password);
        this.hostURL = hostURL;
    }

    // get all the todos
    public TodoCollection getTodos() {
        String url = hostURL + "/todos/api/v1.0/todos";
        try {
            String response = requests.get(url);
            return gson.fromJson(response, TodoCollection.class);
        } catch (IOException e) {
            System.out.println("Unable to get todos");
        }
        return null;
    }

    // create a new todo
    public Todo createTodo(String title, String body, int priority) {
        Todo newTodo = new Todo(title, body, priority);
        String url = hostURL + "/todos/api/v1.0/todo/create";
        String contentType  = "application/json";
        String postData = gson.toJson(newTodo);
        try {
            requests.post(url, contentType, postData);
            return newTodo;
        } catch (IOException e) {
            System.out.println("Unable to create new task: " + title);
        }
        return null;
    }

    // get a todo by uuid
    public Todo getTodo(String uuid) {
        String url = hostURL + "/todos/api/v1.0/todo/" + uuid;
        try {
            String response = requests.get(url);
            return gson.fromJson(response, Todo.class);
        } catch (IOException e) {
            System.out.println("Unable to get todo with id" + uuid);
        }
        return null;
    }

    // get first todo that matches title
    public Todo getTodoByTitle(String title) {
        TodoCollection todos = getTodos();
        for (Todo todo: todos) {
            if (todo.getTitle().equals(title)) {
                return todo;
            }
        }
        return null;
    }

    // delete a todo by uuid
    public boolean removeTodo(String uuid) {
        String url = hostURL + "/todos/api/v1.0/todo/delete/" + uuid;
        try {
            requests.delete(url);
            return true;
        }
        catch (IOException e) {
            System.out.println("Unable to delete todo with ID " + uuid);
        }
        return false;
    }

}

public class Main {
    public static void main(String[] args) {
        TodoAPIWrapper todoAPI = new TodoAPIWrapper("evelynk", "secure",
                "http://csci1660.xaox.net:5000");

        // create two todos
        System.out.println("Adding todos");
        todoAPI.createTodo("Study", "Study for exam", 2);
        todoAPI.createTodo("Dinner", "Prepare dinner", 3);

        // get todos and list them
        System.out.println("Getting todos");
        TodoCollection todos = todoAPI.getTodos();
        for (Todo todo: todos) {
            System.out.println(todo);
        }

        // remove todo with uuid
        System.out.println("Removing todo");
        todoAPI.removeTodo(todos.get(0).getUuid());

        //get remaining todos and list them
        System.out.println("Getting remaining todos");
        todos = todoAPI.getTodos();
        for (Todo todo: todos) {
            System.out.println(todo);
        }

    }
}