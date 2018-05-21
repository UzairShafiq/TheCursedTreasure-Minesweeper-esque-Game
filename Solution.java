// For Uzair Shafiq, uzair.shafiq@uwaterloo.ca, application to the Shopify Backend Developer Intern (Fall 2018) position :)

import java.io.*;
import java.util.*;
import java.text.*;
import java.math.*;
import java.util.regex.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.net.*;

class Input {
    private Integer id;
    private String discount_type;
    private Double discount_value; // Assuming discount values are Integers, based on cart API and sample inputs
    // Only one of three should be specified, but each field exists for gson processing based on reflection
    private String collection;
    private Double product_value;
    private Double cart_value;
    
    public Input() {}
    
    public Integer getID() { return this.id; } 
    public String getDiscountType() { return this.discount_type; }
    public Double getDiscountValue() { return this.discount_value; }
    public String getCollection() { return this.collection; }
    public Double getProductValue() { return this.product_value; }
    public Double getCartValue() { return this.cart_value; }
    
    
    public String getKey() { // Determine which discount-value-key is specified
        String key;
        if (this.collection != null) 
            key = this.collection;
        else if (this.product_value != null) 
            key = "product_value";
        else 
            key = "cart_value";
        return key;
    }
    
    public String toString() { // Useful for debugging
        return "ID:" + this.id + " discountType:" + this.discount_type + 
        " discountValue:" + this.discount_value + " key:" + this.getKey();
    }
    
}

class Product {
    private String name;
    private Double price;
    private String collection;
    
    public Product() {}
    
    public String getName() { return this.name; }
    public Double getPrice() { return this.price; }
    public String getCollection() { return this.collection; }
    
    public void setPrice(Double price) { this.price = price; }
}

class Pagination {
    private Integer current_page;
    private Integer per_page;
    private Integer total;
    
    private Pagination() {}
    
    public Integer getCurrentPage() { return current_page; }
    public Integer getPerPage() { return per_page; }
    public Integer getTotal() { return total; }
}

class CartAPI {
    private List<Product> products;
    private Pagination pagination;
    
    public CartAPI() {}
    
    public List<Product> getProducts() { return products; }
    public Pagination getPagination() { return pagination; }
}

public class Solution {
    
    public static Gson gson = new GsonBuilder().create();
    
    public static Input parseInput(String jsonInput) {
        return gson.fromJson(jsonInput, Input.class);
    }
    
    public static List<Product> getHTMLRequest(Integer productID) throws Exception {
        List<Product> products = new ArrayList<>();
        Boolean done = false;
        Integer pageNumber = 1;
        Integer itemsProcessed = 0;
        do {
            String urlString = "https://backend-challenge-fall-2018.herokuapp.com/carts.json?id=" 
            + productID + "&page=" + pageNumber;
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            
            String JSON = sb.toString();
            CartAPI temp = gson.fromJson(JSON, CartAPI.class);
            products.addAll(temp.getProducts());
            
            itemsProcessed += temp.getPagination().getPerPage();
            if (itemsProcessed >= temp.getPagination().getTotal())
                done = true;
            pageNumber++;
        } while (!done);
        return products;
    }
    
    public static Double getCartTotal(List<Product> cart) {
        Double sum = 0.0;
        for (Product p : cart) {
            sum += p.getPrice();
        }
        return sum;
    }
    
    public static Double applyCartDiscounts(List<Product> cart, Input inputObject, Double cartTotal) {
        String discountType = inputObject.getDiscountType();
        Double discountValue = inputObject.getDiscountValue();
        String discountKey = inputObject.getKey(); // Criteria for discount ('collection', 'product value', etc)
        
        if (discountType.equals("cart")) {
            if (discountKey.equals("cart_value")) {
                Double cart_value = inputObject.getCartValue();
                if (cartTotal > cart_value)
                    return cartTotal - discountValue;
            } else {
                // Perhaps throw an error? Shouldn't apply discount to products if discount_type is 'cart'...
            }
        } 
        else { // If discount_type is not 'cart', it is presumed to be 'product'
            List<Product> myList = new ArrayList<>();
            if (!discountKey.equals("product_value")) {
                for (Product p : cart) {
                    if ((inputObject.getCollection()).equals(p.getCollection())) { // item eligible for discount
                        Double price = p.getPrice();
                        if (price <= discountValue) { p.setPrice(0.0); }
                        else { p.setPrice(price - discountValue); }
                    }
                    myList.add(p);
                }
            } 
            else { // discountKey == 'product_value'
                for (Product p : cart) {
                    Double price = p.getPrice();
                    if (!(price < inputObject.getProductValue())) { 
                        if (price - discountValue < 0) {
                            p.setPrice(0.0);
                        }
                        else {
                            p.setPrice(price - discountValue);    
                        }
                    }
                    myList.add(p);
                }
            }
            return getCartTotal(myList);
        }
        return null; // this'll never happen, one of the above cases must occur
    }
    
    public static String outputFormatter(Double total_amount, Double total_after_discount) {
        StringBuilder sb = new StringBuilder();
        sb.append("{").append("\n").append("  ");
        sb.append("\"total_amount\": ").append(total_amount).append(",").append("\n").append("  ");
        sb.append("\"total_after_discount\": ").append(total_after_discount).append("\n");
        sb.append("}");
        return sb.toString();
    }
    
    public static void main(String args[] ) throws Exception {
        /* Enter your code here. Read input from STDIN. Print output to STDOUT */
        Scanner scanner = new Scanner(System.in);
        StringBuilder sb = new StringBuilder();
        while (scanner.hasNext()) { // Iteration to deal with input on multiple lines
            sb.append(scanner.nextLine());
        }
        Input inputObject = parseInput(sb.toString()); // Stores the fields from the input, like a C++ struct
        List<Product> thingsInCart = getHTMLRequest(inputObject.getID());
        Double total_amount = getCartTotal(thingsInCart); // Total of the cart before discounts applied
        Double total_after_discount = applyCartDiscounts(thingsInCart, inputObject, total_amount);
        System.out.println(outputFormatter(total_amount, total_after_discount));
    }
}