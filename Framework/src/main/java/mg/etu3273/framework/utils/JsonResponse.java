package mg.etu3273.framework.utils;

public class JsonResponse {
    private String status;  
    private int code;       
    private Object data;    
    private Object result;  
    private Integer count;  
    private String message; 
    
    public JsonResponse() {
    }
    
    public static JsonResponse success(Object data) {
        JsonResponse response = new JsonResponse();
        response.status = "success";
        response.code = 200;
        response.data = data;
        return response;
    }
    
    public static JsonResponse successList(Object list, int count) {
        JsonResponse response = new JsonResponse();
        response.status = "success";
        response.code = 200;
        response.result = list;
        response.count = count;
        return response;
    }

    public static JsonResponse error(int code, String message) {
        JsonResponse response = new JsonResponse();
        response.status = "error";
        response.code = code;
        response.message = message;
        return response;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}