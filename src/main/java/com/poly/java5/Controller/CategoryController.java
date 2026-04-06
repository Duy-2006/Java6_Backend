package com.poly.java5.Controller;
import com.poly.java5.Entity.Book;
import com.poly.java5.Entity.Category;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/category")
public class CategoryController {
	 @PersistenceContext
	    private EntityManager em;

	    @GetMapping("/{id}")
	    public String viewCategory(@PathVariable("id") Integer id, Model model) {

	        Category category = em.find(Category.class, id);

	        if (category == null) {
	            return "redirect:/";
	        }

	        List<Book> books = em.createQuery(
	                "SELECT b FROM Book b WHERE b.category.id = :cid",
	                Book.class)
	                .setParameter("cid", id)
	                .getResultList();

	        model.addAttribute("category", category);
	        model.addAttribute("books", books);

	        return "categorie"; // tên file html
	    }

}
