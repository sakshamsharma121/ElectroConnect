package com.example.mystore;

import java.io.File;
import java.io.InputStream;
import java.nio.file.*;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequestMapping("/products")
public class MyStoreController {

    @Autowired
    private MyStoreRepository repo;

    @GetMapping("/")
    public String home() {
        return "/static/index";
    }



    @GetMapping({ "" })
    public String showProductList(Model model) {
        List<Product> products = repo.findAll(Sort.by(Sort.Direction.DESC, "id"));
        model.addAttribute("products", products);
        return "products/index";
    }

    @GetMapping("/create")
    public String showCreatePage(Model model) {
        ProductDto productDto = new ProductDto();
        model.addAttribute("productDto", productDto);
        return "products/CreateProduct";
    }

    @PostMapping("/create")
    public String createProduct(
            @Valid @ModelAttribute ProductDto productDto,
            BindingResult result) {

        if (productDto.getImageFile().isEmpty()) {
            result.addError(new FieldError("productDto", "ImageFile", "The file is required"));
        }

        if (result.hasErrors()) {
            return "products/CreateProduct";
        }

        MultipartFile image = productDto.getImageFile();
        Date createdAt = new Date();
        String storageFileName = createdAt.getTime() + "_" + image.getOriginalFilename();

        try {
            String uploadDir = "public/images"; // Ensure this path is correct and accessible
            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            try (InputStream inputStream = image.getInputStream()) {
                Path filePath = uploadPath.resolve(storageFileName); // Use resolve for path concatenation
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception ex) {
            System.out.println("Exception while saving file: " + ex.getMessage());
            ex.printStackTrace();
            // Optionally, rethrow the exception or handle it according to your
            // application's requirements
        }

        Product product = new Product();
        product.setName(productDto.getName());
        product.setBrand(productDto.getBrand());
        product.setCategory(productDto.getCategory());
        product.setPrice(productDto.getPrice());
        product.setDescription(productDto.getDescription());
        product.setcreatedAt(createdAt);
        product.setImageFileName(storageFileName);

        repo.save(product);

        return "redirect:/products";
    }

    @GetMapping("/edit")
    public String showEditPage(
            Model model,
            @RequestParam int id) {

        try {
            Product product = repo.findById(id).get();
            model.addAttribute("product", product);

            ProductDto productDto = new ProductDto();
            productDto.setName(product.getName());
            productDto.setBrand(product.getBrand());
            productDto.setCategory(product.getCategory());
            productDto.setPrice(product.getPrice());
            productDto.setDescription(product.getDescription());

            model.addAttribute("productDto", productDto);

        } catch (Exception ex) {
            System.out.println("Exception:" + ex.getMessage());
            return "redirect:/products";
        }

        return "products/EditProduct";
    }

    @PostMapping("/edit")
    public String updateProduct(Model model, @RequestParam int id,
            @Valid @ModelAttribute ProductDto productDto,
            BindingResult result) {
        try {
            Optional<Product> optionalProduct = repo.findById(id);
            if (!optionalProduct.isPresent()) {
                model.addAttribute("error", "Product not found");
                return "products/EditProduct";
            }
    
            Product product = optionalProduct.get();
            model.addAttribute("product", product);
    
            if (result.hasErrors()) {
                return "products/EditProduct";
            }
    
            if (!productDto.getImageFile().isEmpty()) {
                String uploadDir = "public" + File.separator + "images";
                Path oldImagePath = Paths.get(uploadDir, product.getImageFileName());
    
                try {
                    Files.deleteIfExists(oldImagePath);
                } catch (Exception ex) {
                    System.out.println("Exception while deleting old image: " + ex.getMessage());
                }
    
                MultipartFile image = productDto.getImageFile();
                Date createdAt = new Date();
                String storageFileName = createdAt.getTime() + "_" + image.getOriginalFilename();
    
                try (InputStream inputStream = image.getInputStream()) {
                    Path newImagePath = Paths.get(uploadDir, storageFileName);
                    Files.copy(inputStream, newImagePath, StandardCopyOption.REPLACE_EXISTING);
                    product.setImageFileName(storageFileName);
                } catch (Exception ex) {
                    System.out.println("Exception while saving new image: " + ex.getMessage());
                    model.addAttribute("error", "Image upload failed");
                    return "products/EditProduct";
                }
            }
    
            product.setName(productDto.getName());
            product.setBrand(productDto.getBrand());
            product.setCategory(productDto.getCategory());
            product.setPrice(productDto.getPrice());
            product.setDescription(productDto.getDescription());
    
            repo.save(product);
    
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
            model.addAttribute("error", "An unexpected error occurred");
            return "products/EditProduct";
        }
    
        return "redirect:/products";
    }
    

    @GetMapping("/delete")
    public String deleteProduct(
        @RequestParam int id){

            try{
                Product product= repo.findById(id).get();

                Path imagePath =Paths.get("public/images/" + product.getImageFileName());

                try{
                    Files.delete(imagePath);
                }
                catch(Exception ex){
                    System.out.println("Exception:" + ex.getMessage());
                }

                repo.delete(product);
            }

            catch(Exception ex){
                System.out.println("Exception:" + ex.getMessage());
            }

            return "redirect:/products";
    }

}
