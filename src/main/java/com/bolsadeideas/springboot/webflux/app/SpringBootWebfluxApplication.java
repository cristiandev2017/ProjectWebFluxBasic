package com.bolsadeideas.springboot.webflux.app;

import com.bolsadeideas.springboot.webflux.app.models.documents.Categoria;
import com.bolsadeideas.springboot.webflux.app.models.documents.Producto;
import com.bolsadeideas.springboot.webflux.app.models.services.ProductoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import reactor.core.publisher.Flux;

import java.util.Date;

@SpringBootApplication
public class SpringBootWebfluxApplication implements CommandLineRunner {
    @Autowired
    private ProductoService productoService;

    @Autowired
    private ReactiveMongoTemplate mongoTemplate;

    private static final Logger log = LoggerFactory.getLogger(SpringBootWebfluxApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SpringBootWebfluxApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        mongoTemplate.dropCollection("productos").subscribe();
        mongoTemplate.dropCollection("categorias").subscribe();

        Categoria electronico = new Categoria("Electronico");
        Categoria deportes = new Categoria("Deportes");
        Categoria computacion = new Categoria("Computacion");
        Categoria muebles = new Categoria("Muebles");

        Flux.just(electronico, deportes, computacion, muebles)
                .flatMap(productoService::saveCategoria)
                .doOnNext(categoria -> {
                    log.info("Categoria creada: "
                            + categoria.getNombre()
                            + "Id: "
                            + categoria.getId()
                    );
                }).thenMany(Flux.just(new Producto("TV Panasonic Pantalla LCD", 456.89, electronico),
                        new Producto("Sony Camara HD Digital", 177.89, electronico),
                        new Producto("Apple iPad", 46.89, electronico),
                        new Producto("Sony Notebook", 876.89, computacion),
                        new Producto("Hewlett Packard Multifuncional", 200.89, computacion),
                        new Producto("TV Sony Bavia OLED 4K", 2255.89, electronico),
                        new Producto("Silla Gamer Luz LED", 900.89, muebles),
                        new Producto("Escritorio SUPER NOVA", 900.89, muebles),
                        new Producto("Bicicleta HNO", 900.89, deportes),
                        new Producto("Balon de Futbol Golty", 6.89, deportes)
                ).flatMap(producto -> {
                    producto.setCreateAt(new Date());
                    return productoService.save(producto);
                }))
                .subscribe(producto -> log.info("Insert: " + producto.getId() + " " + producto.getNombre()));
    }
}
