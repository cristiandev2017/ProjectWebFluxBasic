package com.bolsadeideas.springboot.webflux.app.controllers;

import com.bolsadeideas.springboot.webflux.app.models.documents.Categoria;
import com.bolsadeideas.springboot.webflux.app.models.documents.Producto;
import com.bolsadeideas.springboot.webflux.app.models.services.ProductoService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.thymeleaf.spring6.context.webflux.ReactiveDataDriverContextVariable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Date;

@SessionAttributes("producto")
@Controller
public class ProductoController {

    @Autowired
    private ProductoService productoService;

    private static final Logger log = LoggerFactory.getLogger(ProductoController.class);

    @ModelAttribute("categorias")
    public Flux <Categoria> categorias() {
        return productoService.findAllCategoria();
    }

    @GetMapping({"/listar", "/"})
    public String listar(Model model) {
        Flux <Producto> productos = productoService.findAllConMayusculas();
        productos.subscribe(producto -> log.info(producto.getNombre()));

        model.addAttribute("productos", productos);
        model.addAttribute("titulo", "Listado de productos");
        return "listar";
    }

    @GetMapping("/listar-datadriver")
    public Mono <String> listarDataDriver(Model model) {
        Flux <Producto> productos = productoService.findAllConMayusculas()
                .delayElements(Duration.ofSeconds(1));
        productos.subscribe(producto -> log.info(producto.getNombre()));

        model.addAttribute("productos", new ReactiveDataDriverContextVariable(productos, 2));
        model.addAttribute("titulo", "Listado de productos");
        return Mono.just("listar");
    }

    @GetMapping("/form")
    public Mono <String> crear(Model model) {
        model.addAttribute("producto", new Producto());
        model.addAttribute("titulo", "Formulario de producto");
        model.addAttribute("boton", "Guardar");
        return Mono.just("form");
    }

    @GetMapping("/form/{id}")
    public Mono <String> editar(@PathVariable String id, Model model) {
        Mono <Producto> productoMono = productoService.findById(id)
                .doOnNext(producto -> log.info(producto.getNombre()))
                .defaultIfEmpty(new Producto());
        model.addAttribute("boton", "Editar");
        model.addAttribute("titulo", "Editar Producto");
        model.addAttribute("producto", productoMono);

        return Mono.just("form");
    }

    @GetMapping("/form-v2/{id}")
    public Mono <String> editarV2(@PathVariable String id, Model model) {


        return productoService.findById(id).doOnNext(p -> {
                    log.info("Producto: " + p.getNombre());
                    model.addAttribute("boton", "Editar");
                    model.addAttribute("titulo", "Editar Producto");
                    model.addAttribute("producto", p);
                }).defaultIfEmpty(new Producto())
                .flatMap(p -> {
                    if (p.getId() == null) {
                        return Mono.error(new InterruptedException("No extiste el producto"));
                    }
                    return Mono.just(p);
                })
                .then(Mono.just("form"))
                .onErrorResume(ex -> Mono.just("redirect:/listar?error=no+existe+el+producto"));
    }

    @PostMapping("/form")
    public Mono <String> guardar(@Valid Producto producto, BindingResult result, Model model, SessionStatus status) {
        if (result.hasErrors()) {
            model.addAttribute("titulo", "Errores en formulario producto");
            model.addAttribute("boton", "Guardar");
            return Mono.just("form");
        } else {
            status.setComplete();
            Mono <Categoria> categoria = productoService.findCategoriaById(producto.getCategoria().getId());

            return categoria.flatMap(categoriaProducto -> {
                if (producto.getCreateAt() == null) {
                    producto.setCreateAt(new Date());
                }
                producto.setCategoria(categoriaProducto);
                return productoService.save(producto);
            }).doOnNext(productoGuardado -> {
                log.info("Categoria asignada : " + productoGuardado.getCategoria().getNombre());
                log.info("Producto guardado : "
                        + productoGuardado.getNombre()
                        + "Id:" + productoGuardado.getId());
            }).thenReturn("redirect:/listar?success=producto+guardado+con+exito");
        }
    }


    @GetMapping("/listar-full")
    public String listarFull(Model model) {
        Flux <Producto> productos = productoService.findAllConMayusculasConRepeat();

        model.addAttribute("productos", productos);
        model.addAttribute("titulo", "Listado de productos");
        return "listar";
    }

    @GetMapping("/listar-chunked")
    public String listarChunked(Model model) {

        Flux <Producto> productos = productoService.findAllConMayusculasConRepeat();

        model.addAttribute("productos", productos);
        model.addAttribute("titulo", "Listado de productos");
        return "listar-chunked";
    }

    @GetMapping("/eliminar/{id}")
    public Mono <String> eliminar(@PathVariable String id) {
        return productoService.findById(id)
                .defaultIfEmpty(new Producto())
                .flatMap(p -> {
                    if (p.getId() == null) {
                        return Mono.error(new InterruptedException("No extiste el producto a eliminar"));
                    }
                    return Mono.just(p);
                })
                .flatMap(producto -> productoService.delete(producto))
                .then(Mono.just("redirect:/listar?success=producto+eliminado+con+exito"))
                .onErrorResume(ex -> Mono.just("redirect:/listar?error=no+existe+el+producto"));
    }


}
