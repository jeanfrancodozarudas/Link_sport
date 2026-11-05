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

    const escapar = valor => String(valor).replace(/[&<>"']/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[char]);
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

    window.anadirProductoAMemoria = function (id, nombre, precio, imagen, stock) {
        if (!Number.isInteger(stock) || stock <= 0) { alert('Este producto está agotado.'); return; }
        const productoExistente = productosEnCarrito.find(item => item.id === id);
        if (productoExistente) { if (productoExistente.cantidad >= Math.min(99, stock)) { alert('No hay más unidades disponibles de este producto.'); return; } productoExistente.cantidad++; }
        else { productosEnCarrito.push({ id: id, nombre: nombre, precio: precio, imagen: imagen, cantidad: 1, stock: stock }); }
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
            itemHTML.innerHTML = `<img src="${escapar(producto.imagen)}" class="carrito-item-img mr-3"><div style="flex-grow:1;"><h4 class="is-size-7 has-text-weight-bold has-text-dark mb-1">${escapar(producto.nombre)}</h4><p class="is-size-7 has-text-danger has-text-weight-bold mb-2">S/. ${producto.precio.toFixed(2)}</p><div class="is-flex is-align-items-center"><button class="btn-cantidad-control" onclick="modificarCantidadItem(${indice},-1)">-</button><span class="px-3 is-size-7 has-text-weight-bold">${producto.cantidad}</span><button class="btn-cantidad-control" onclick="modificarCantidadItem(${indice},1)">+</button></div></div><button class="button is-small is-text has-text-grey" onclick="eliminarItemCompleto(${indice})" style="position:absolute;right:0;top:35%;"><span class="icon"><i class="fas fa-trash-can"></i></span></button>`;
            listaProductosCart.appendChild(itemHTML);
        });
        txtSubtotalCart.textContent = `S/. ${totalCalculado.toFixed(2)}`;
        burbujaContador.textContent = unidadesTotales;
        burbujaContador.style.display = "block";
    }

    window.modificarCantidadItem = function (indice, cambio) {
        if (!productosEnCarrito[indice]) return;
        if (cambio > 0 && productosEnCarrito[indice].cantidad >= Math.min(99, productosEnCarrito[indice].stock)) { alert('No hay más unidades disponibles de este producto.'); return; }
        productosEnCarrito[indice].cantidad = Math.min(99, productosEnCarrito[indice].stock, productosEnCarrito[indice].cantidad + cambio);
        if (productosEnCarrito[indice].cantidad <= 0) { productosEnCarrito.splice(indice, 1); }
        actualizarPantallaCarrito();
    };
    window.eliminarItemCompleto = function (indice) {
        productosEnCarrito.splice(indice, 1);
        actualizarPantallaCarrito();
    };
    window.linkCheckout = {
        items: () => productosEnCarrito.map(p => ({ ...p })),
        clear: () => { productosEnCarrito = []; actualizarPantallaCarrito(); }
    };
    // 5. FORMULARIO DE SUSCRIPCIÓN TRADICIONAL
    const formSuscripcion = document.getElementById('form-suscripcion');
    const modalConfirm = document.getElementById('modal-confirmacion');
    if (formSuscripcion && modalConfirm) {
        formSuscripcion.addEventListener('submit', (evento) => {
            evento.preventDefault();
            const nombre = document.getElementById('input-nombre').value.trim();
            const correo = document.getElementById('input-correo').value.trim();
            document.getElementById('modal-mensaje-texto').textContent = `Hola ${nombre}. Recibimos tus datos de suscripción: ${correo}.`;
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

                window.anadirProductoAMemoria(Number(tarjetaProducto.dataset.productoId), nombre, Number(tarjetaProducto.dataset.precio), imagenSrc, Number(tarjetaProducto.dataset.stock));
                window.abrirPanelCarrito();
            });
        });
    }
    // Retraso de seguridad para esperar el renderizado asíncrono de base de datos de Spring Boot
    setTimeout(activarEventosCarritoThymeleaf, 600);

});
