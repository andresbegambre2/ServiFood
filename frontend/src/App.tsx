import './App.css'

function App() {
  return (
    <main>
      <nav className="navigation" aria-label="Navegación principal">
        <a className="brand" href="/" aria-label="ServiFood, inicio">SERVI<span>FOOD</span></a>
        <span className="status"><i aria-hidden="true" /> Próximamente</span>
      </nav>
      <section className="hero">
        <div className="hero__content">
          <p className="eyebrow">Sabor local · Experiencia digital</p>
          <h1>Tu antojo,<br /><em>sin vueltas.</em></h1>
          <p className="intro">Estamos construyendo una nueva forma de pedir, preparar y disfrutar tus hamburguesas favoritas.</p>
          <div className="actions">
            <a className="button" href="#about">Conocer ServiFood <span>↘</span></a>
            <p>Pedidos simples.<br />Cocina en movimiento.</p>
          </div>
        </div>
        <div className="hero__art" aria-hidden="true">
          <div className="orbit orbit--one" /><div className="orbit orbit--two" />
          <div className="burger-mark"><span className="bun bun--top" /><span className="filling filling--cheese" /><span className="filling filling--patty" /><span className="bun bun--bottom" /></div>
          <span className="stamp">HECHO<br />CON<br />ACTITUD</span>
        </div>
      </section>
      <section className="promise" id="about">
        <p>01</p><h2>Una plataforma.<br />Toda la operación.</h2><p>Cliente, caja y cocina conectados para servir mejor.</p>
      </section>
    </main>
  )
}

export default App
