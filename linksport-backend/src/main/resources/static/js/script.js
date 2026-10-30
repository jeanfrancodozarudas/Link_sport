document.addEventListener('DOMContentLoaded', () => {

    // 1. MENÚ RESPONSIVO (BOTÓN HAMBURGUESA BULMA)
    const burgerButton = document.getElementById('burger-button');
    const navbarMenu = document.getElementById('navbar-menu-clothes');
    if (burgerButton && navbarMenu) {
        burgerButton.addEventListener('click', () => {
            burgerButton.classList.toggle('is-active');
            navbarMenu.classList.toggle('is-active');
        });
    }

    // 2. LÓGICA DEL CARRUSEL DE IMÁGENES (BANNERS)
    let currentSlideIndex = 0;
    const slides = document.querySelectorAll('.carousel-item');
    window.nextSlide = function () {
        changeSlideVisibility(currentSlideIndex, false);
        currentSlideIndex = (currentSlideIndex + 1) % slides.length;
        changeSlideVisibility(currentSlideIndex, true);
    };
    window.prevSlide = function () {
        changeSlideVisibility(currentSlideIndex, false);
        currentSlideIndex = (currentSlideIndex - 1 + slides.length) % slides.length;
        changeSlideVisibility(currentSlideIndex, true);
    };
    function changeSlideVisibility(index, isVisible) {
        if (slides[index]) {
            if (isVisible) {
                slides[index].classList.add('is-active');
                slides[index].style.display = 'flex';
            } else {
                slides[index].classList.remove('is-active');
                slides[index].style.display = 'none';
            }
        }
    }
    let autoPlayInterval = setInterval(window.nextSlide, 4000);
    const carouselBox = document.querySelector('.hero-banner, .box');
    if (carouselBox) {
        carouselBox.addEventListener('mouseenter', () => clearInterval(autoPlayInterval));
        carouselBox.addEventListener('mouseleave', () => autoPlayInterval = setInterval(window.nextSlide, 4000));
    }

    // 3. MOTOR INTERNO DEL CARRITO LATERAL (SIDEBAR)
    const sidebarCart = document.getElementById('carrito-sidebar');
    const overlayCart = document.getElementById('carrito-overlay');
    const btnAbrirCart = document.getElementById('btn-abrir-carrito');
    const btnCerrarCart = document.getElementById('btn-cerrar-carrito');
    const listaProductosCart = document.getElementById('carrito-productos-lista');
    const txtSubtotalCart = document.getElementById('carrito-subtotal-valor');
    const burbujaContador = document.getElementById('carrito-burbuja-contador');
    let productosEnCarrito = [];

    window.abrirPanelCarrito = function () {
        if (sidebarCart && overlayCart) {
            sidebarCart.classList.add('is-active');
            overlayCart.classList.add('is-active');
        }
    };
    window.cerrarPanelCarrito = function () {
        if (sidebarCart && overlayCart) {
            sidebarCart.classList.remove('is-active');
            overlayCart.classList.remove('is-active');
        }
    };
    if (btnAbrirCart) btnAbrirCart.addEventListener('click', window.abrirPanelCarrito);
    if (btnCerrarCart) btnCerrarCart.addEventListener('click', window.cerrarPanelCarrito);
    if (overlayCart) overlayCart.addEventListener('click', window.cerrarPanelCarrito);

    window.anadirProductoAMemoria = function (nombre, precio, imagen) {
        const productoExistente = productosEnCarrito.find(item => item.nombre === nombre);
        if (productoExistente) { productoExistente.cantidad++; }
        else { productosEnCarrito.push({ nombre: nombre, precio: precio, imagen: imagen, cantidad: 1 }); }
        actualizarPantallaCarrito();
    };

    function actualizarPantallaCarrito() {
        if (!listaProductosCart || !txtSubtotalCart || !burbujaContador) return;
        listaProductosCart.innerHTML = "";
        if (productosEnCarrito.length === 0) {
            listaProductosCart.innerHTML = `<div id="carrito-vacio-mensaje" class="has-text-centered has-text-grey py-6"><span class="icon is-large"><i class="fas fa-cart-shopping fa-2x mb-3"></i></span><p>Tu carrito está vacío.</p></div>`;
            txtSubtotalCart.textContent = "S/. 0.00";
            burbujaContador.style.display = "none";
            return;
        }
        let totalCalculado = 0; let unidadesTotales = 0;
        productosEnCarrito.forEach((producto, indice) => {
            totalCalculado += producto.precio * producto.cantidad;
            unidadesTotales += producto.cantidad;
            const itemHTML = document.createElement('div');
            itemHTML.className = "carrito-item-box py-3 mb-2";
            itemHTML.innerHTML = `<img src="${producto.imagen}" class="carrito-item-img mr-3"><div style="flex-grow:1;"><h4 class="is-size-7 has-text-weight-bold has-text-dark mb-1">${producto.nombre}</h4><p class="is-size-7 has-text-danger has-text-weight-bold mb-2">S/. ${producto.precio.toFixed(2)}</p><div class="is-flex is-align-items-center"><button class="btn-cantidad-control" onclick="modificarCantidadItem(${indice},-1)">-</button><span class="px-3 is-size-7 has-text-weight-bold">${producto.cantidad}</span><button class="btn-cantidad-control" onclick="modificarCantidadItem(${indice},1)">+</button></div></div><button class="button is-small is-text has-text-grey" onclick="eliminarItemCompleto(${indice})" style="position:absolute;right:0;top:35%;"><span class="icon"><i class="fas fa-trash-can"></i></span></button>`;
            listaProductosCart.appendChild(itemHTML);
        });
        txtSubtotalCart.textContent = `S/. ${totalCalculado.toFixed(2)}`;
        burbujaContador.textContent = unidadesTotales;
        burbujaContador.style.display = "block";
    }

    window.modificarCantidadItem = function (indice, cambio) {
        productosEnCarrito[indice].cantidad += cambio;
        if (productosEnCarrito[indice].cantidad <= 0) { productosEnCarrito.splice(indice, 1); }
        actualizarPantallaCarrito();
    };
    window.eliminarItemCompleto = function (indice) {
        productosEnCarrito.splice(indice, 1);
        actualizarPantallaCarrito();
    };
    // 4. MODAL DE PROCEDER AL PAGO (CONEXIÓN DIRECTA A POSTGRESQL)
    const btnPago = document.getElementById('btn-proceder-pago');
    const modalCompra = document.getElementById('modal-compra-exitosa');
    if (btnPago && modalCompra) {
        btnPago.addEventListener('click', () => {
            if (productosEnCarrito.length === 0) { alert("Tu carrito está vacío."); return; }

            // BUSCAMOS LOS IDs REALES DE LAS FILAS DE THYMELEAF
            const tarjetas = document.querySelectorAll('#productos .card');
            const mapeoProductos = productosEnCarrito.map(item => {
                let idEncontrado = 1; // ID por defecto
                tarjetas.forEach((tarjeta, indice) => {
                    const nombreTarjeta = tarjeta.querySelector('h3').textContent.trim();
                    if (nombreTarjeta === item.nombre) {
                        idEncontrado = indice + 1; // Asocia el ID correlativo de la BD (1, 2, 3, 4)
                    }
                });
                return {
                    id: idEncontrado,
                    nombre: item.nombre,
                    precio: item.precio,
                    cantidad: item.cantidad,
                    imagenSrc: item.imagen
                };
            });

            // ENVIAMOS LA ORDEN REAL MEDIANTE FETCH AL BACKEND DE SPRING BOOT
            fetch('/api/productos/procesar-pago', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(mapeoProductos)
            })
                .then(res => {
                    if (!res.ok) throw new Error("Error procesando pago");
                    window.cerrarPanelCarrito();
                    modalCompra.classList.add('is-active');
                    productosEnCarrito = [];
                    actualizarPantallaCarrito();
                    console.log("¡Compra registrada automáticamente en las tablas de PostgreSQL!");
                })
                .catch(err => alert("Error al registrar la transacción en la base de datos."));
        });

        const btnAceptarCompra = document.getElementById('modal-compra-btn-aceptar');
        const btnXCompra = document.getElementById('modal-compra-x-close');
        const bgCloseCompra = document.getElementById('modal-compra-bg-close');
        const cerrarModalCompra = () => modalCompra.classList.remove('is-active');
        if (btnAceptarCompra) btnAceptarCompra.addEventListener('click', cerrarModalCompra);
        if (btnXCompra) btnXCompra.addEventListener('click', cerrarModalCompra);
        if (bgCloseCompra) bgCloseCompra.addEventListener('click', cerrarModalCompra);
    }
    // 5. FORMULARIO DE SUSCRIPCIÓN TRADICIONAL
    const formSuscripcion = document.getElementById('form-suscripcion');
    const modalConfirm = document.getElementById('modal-confirmacion');
    if (formSuscripcion && modalConfirm) {
        formSuscripcion.addEventListener('submit', (evento) => {
            evento.preventDefault();
            const nombre = document.getElementById('input-nombre').value.trim();
            const correo = document.getElementById('input-correo').value.trim();
            document.getElementById('modal-mensaje-texto').innerHTML = `¡Hola <strong>${nombre}</strong>! Bienvenido a LINKSPORT. Cupones enviados a <strong>${correo}</strong>.`;
            modalConfirm.classList.add('is-active');
            formSuscripcion.reset();
        });
        const btnAceptar = document.getElementById('modal-btn-aceptar');
        const btnX = document.getElementById('modal-x-close');
        const bgClose = document.getElementById('modal-bg-close');
        const cerrarModal = () => modalConfirm.classList.remove('is-active');
        if (btnAceptar) btnAceptar.addEventListener('click', cerrarModal);
        if (btnX) btnX.addEventListener('click', cerrarModal);
        if (bgClose) bgClose.addEventListener('click', cerrarModal);
    }

    // 6. CONTROL DE LOGIN Y REGISTRO (CONECTADOS)
    const modalLogin = document.getElementById('modal-login-oferta');
    const modalReg = document.getElementById('modal-registro-usuario');
    const btnAbrirLogin = document.getElementById('btn-usuario-login');
    const btnXLogin = document.getElementById('modal-login-x-close');
    const bgCloseLogin = document.getElementById('modal-login-bg-close');
    const formLogin = document.getElementById('form-login-modal');
    const linkIrARegistro = document.getElementById('link-ir-a-registro');
    const linkIrALogin = document.getElementById('link-ir-a-login');
    const btnXReg = document.getElementById('modal-reg-x-close');
    const bgCloseReg = document.getElementById('modal-reg-bg-close');
    const formRegistro = document.getElementById('form-registro-modal');
    const abrirLogin = () => modalLogin.classList.add('is-active');
    const cerrarLogin = () => modalLogin.classList.remove('is-active');
    const abrirRegistro = () => modalReg.classList.add('is-active');
    const cerrarRegistro = () => modalReg.classList.remove('is-active');

    setTimeout(() => { if (modalLogin) abrirLogin(); }, 2000);
    if (btnAbrirLogin) btnAbrirLogin.addEventListener('click', (e) => { e.preventDefault(); abrirLogin(); });
    if (btnXLogin) btnXLogin.addEventListener('click', cerrarLogin);
    if (bgCloseLogin) bgCloseLogin.addEventListener('click', cerrarLogin);
    if (linkIrARegistro) linkIrARegistro.addEventListener('click', (e) => { e.preventDefault(); cerrarLogin(); abrirRegistro(); });
    if (linkIrALogin) linkIrALogin.addEventListener('click', (e) => { e.preventDefault(); cerrarRegistro(); abrirLogin(); });
    if (btnXReg) btnXReg.addEventListener('click', cerrarRegistro);
    if (bgCloseReg) bgCloseReg.addEventListener('click', cerrarRegistro);

    if (formLogin) {
        formLogin.addEventListener('submit', (e) => {
            e.preventDefault();
            alert("¡Sesión Iniciada con éxito!"); cerrarLogin(); formLogin.reset();
        });
    }
    if (formRegistro) {
        formRegistro.addEventListener('submit', (e) => {
            e.preventDefault();
            alert("¡Registro Exitoso!"); cerrarRegistro(); formRegistro.reset();
        });
    }

    // 7. DETECTOR DE CLICS CORREGIDO COMPATIBLE CON THYMELEAF Y REJILLAS BULMA
    function activarEventosCarritoThymeleaf() {
        // Buscamos de forma directa los botones negros de añadir dentro de las tarjetas del catálogo
        const botonesAnadir = document.querySelectorAll('#productos .card .button, .card-content .button');
        botonesAnadir.forEach(boton => {
            boton.addEventListener('click', (evento) => {
                const tarjetaProducto = evento.target.closest('.card');
                if (!tarjetaProducto) return;

                const nombre = tarjetaProducto.querySelector('h3').textContent.trim();
                const precioTexto = tarjetaProducto.querySelector('.has-text-danger').textContent.trim();
                const imagenSrc = tarjetaProducto.querySelector('img').getAttribute('src');
                const precio = parseFloat(precioTexto.replace('S/.', '').trim());

                window.anadirProductoAMemoria(nombre, precio, imagenSrc);
                window.abrirPanelCarrito();
            });
        });
    }
    // Retraso de seguridad para esperar el renderizado asíncrono de base de datos de Spring Boot
    setTimeout(activarEventosCarritoThymeleaf, 600);

});
