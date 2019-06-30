public class Nucleo0 extends Thread {
    private int registro[];
    private int quantumTotal;
    private int quantumHililloActual;
    private CacheDatosD cacheDatosLocal;
    private CacheDatosC cacheDatosNucleo1;
    private int PC;
    private int RL;
    private int idHililloActual;
    private Planificador planificador;
    private Pcb pcb;
    private CacheInstrucciones cacheInstrucciones;
    private Lock lockDatosCache0;
    private Lock lockDatosCache1;
    private Lock lockMemoriaDatos;
    private instruccionActual[];

    public Nucleo0(CacheInstrucciones instrucciones, CacheDatosD local, CacheDatosC cacheDatosNucleo1, int quantum, Planificador planificador,
    Lock lockDatosCache0, Lock lockDatosCache1, Lock lockMemoriaDatos){
        this.cacheDatosLocal = local;
        this.cacheDatosNucleo1 = cacheDatosNucleo1;
        this.cacheInstrucciones = instrucciones; 
        registro = new int[32];
        this.planificador = planificador;
        this.lockDatosCache0 = lockDatosCache0;
        this.lockDatosCache1 = lockDatosCache1;
        this.lockMemoriaDatos = lockMemoriaDatos;
    }

    private void copiarPcbAContextoActual(Pcb pcb){
        this.registro = pcb.getRegistro();
        this.idHililloActual = pcb.getId();
        this.PC = pcb.getPc();
        this.RL = -1;
        quantumHililloActual = quantumTotal;
    }

    private obtenerSiguienteInstruccion(){
        int direccion = PC;
        int bloqueMemoria = direccion / 16;
        int palabra = direccion % 16;
        int bloqueCache = bloqueMemoria % 8;
        if(cacheInstrucciones.estaEnCache(bloqueMemoria)){
            cacheInstrucciones.leerInstruccion(direccion, instruccionActual);
        }
        else{
            cacheInstrucciones.leerInstruccion(direccion, instruccionActual);
            for(int i = 0; i < 32; i++){
                cyclicBarrier.await();
                relojNucleo0++;
            }
        }
    }

    // Guarda el contexto del hilo actual y carga el contexto del siguiente hilo
    private void siguienteHilillo(bool termiando, Pcb pcb){
        if(!terminado){
            pcb.setEstado('R');
            pcb.setRegistro(this.registro);
            pcb.setPc(this.PC);
            planificador.agregarProcesosRestantes(pcb);
        }
        else{
            pcb.setEstado('F');
            pcb.setRegistro(this.registro);
            pcb.setPc(this.PC);
            planificador.agregarProcesosTerminados(pcb);
        }
        this.pcb = planificador.usarProcesosRestantes();
        copiarPcbAContextoActual(pcb);
    }

    private int Alu(int operacion, int operando1, int operando2){
        int resultado = -1;
        switch(operacion){
            case 1:
                resultado = operando1 + operando2;
                break;
            case 2:
                resultado = operando1 - operando2;
                break;
            case 3:
                resultado = operando1 * operando2;
                break:
            case 4:
                resultado = operando1 / operando2;
                break;
        }
        return resultado;
    }

    private bool intentarBloqueo(){
        bool respuesta = true;
        if(lockDatosCache0.trylock() == false){
            respuesta = false;
        }
        if(lockDatosCache1.trylock() == false){
            respuesta = false;
        }
        if(lockMemoriaDatos.trylock() == false){
            respuesta = false;
        }
    }

    private void desbloquear(){
        lockDatosCache0.unlock();
        lockDatosCache1.unlock();
        lockMemoriaDatos.unlock();
    }

    private void decodificador(int[] instruccion){
        switch(instruccion[0]){
            case 19:
                registro[instruccion[1]] = Alu(1, registro[instruccion[2]], instruccion[3]);
                break;
            case 71:
                registro[instruccion[1]] = Alu(1, registro[instruccion[2]], instruccion[3]);
                break;
            case 83:
                registro[instruccion[1]] = Alu(2, registro[instruccion[2]], instruccion[3]);
                break;
            case 72:
                registro[instruccion[1]] = Alu(3, registro[instruccion[2]], instruccion[3]);
                break;
            case 56:
                registro[instruccion[1]] = Alu(4, registro[instruccion[2]], instruccion[3]);
                break;
            case 5:
                // Se calcula direccion de memoria y bloques
                int direccion = Alu(1, instruccion[2], instruccion[3]);
                int bloqueMemoria = direccion / 4;
                int palabra = direccion % 4;
                int bloqueCache = bloqueMemoria % 8;
                
                // Se bloquea cache local
                while(intentarBloqueo() == false){
                    cyclicBarrier.await();
                    relojNucleo0++;
                }

                // Se revisa si esta el bloque en cache
                boolean estaEnCache = cacheDatosLocal.existeBloque(bloqueCache, bloqueMemoria);

                if(estaEnCache){
                    // Si está, y no es "I", lee el dato
                    registro[instruccion[1]] = cacheDatosLocal.leerDato(direccion);
                }
                else{ // Si no esta en cache local

                    // Se busca si esta bloque en otra cache
                    boolean estaEnOtraCache = cacheDatosNucleo1.existeBloque(bloqueMemoria);
                        
                    if(!estaEnOtraCache){ // Si no esta en la otra, se trae el dato
                        int dato cacheDatosLocal.leerDato(direccion);
                        for(int i = 0; i < 32; i++){
                            cyclicBarrier.await();
                            relojNucleo0++;
                        }
                        // Guarda el dato en el registro 
                        registro[instruccion[1]] = dato;
                    }
                    else{ // Si esta el bloque en la otra cache(esta en "M" o "C")
                    // No se si hacer lo mismo en ambos casos("M","C")
                        if(estadoOtraCache == 'M'){
                            cacheDatosLocal.cargarBloque(direccion, bloqueCache ,bloqueMemoria);
                            for(int i = 0; i < 32; i++){
                                cyclicBarrier.await();
                                relojNucleo0++;
                            }
                            int dato = cacheDatosLocal.leerDato(direccion);
                            registro[instruccion[1]] = dato;
                        }
                        /*else if(estadoOtraCache == 'C'){
                            int dato = memoria.leerBloqueDatos(direccion, cacheDatosLocal[bloqueCache]);
                            for(int i = 0; i < 32; i++){
                                //cyclicBarrier.await();
                            }
                            registro[instruccion[1]] = dato;
                        }*/
                    }
                }
                // Se desbloquean los 3 recursos
                desbloquear();
                break;
            case 37:
                int direccion = Alu(1, instruccion[1], instruccion[3]);
                int bloqueMemoria = direccion / 4;
                int palabra = direccion % 4;
                int bloqueCache = bloqueMemoria % 8;

                // Se bloquea cache local
                while(intentarBloqueo() == false){
                    cyclicBarrier.await();
                    relojNucleo0++;
                }
                // Se revisa si esta bloque en cache
                boolean estaEnCache = cacheDatosLocal.existeBloque(bloqueCache, bloqueMemoria);
                
                if(estaEnCache){
                    if(estado == 'M'){
                        // Se escribe dato en cache
                        cacheDatosLocal.escribirDato(direccion, instruccion[2]);
                    }
                    else if(estado == 'C'){
                        boolean estaEnOtraCache = cacheDatosNucleo1.existeBloque(bloqueMemoria);
                        if(estaEnOtraCache){
                            //Poner estado otra cache en I
                        }
                        cacheDatosLocal.escribirDato(direccion, instruccion[2]);
                        // Poner estado en "M"
                    }
                }
                else{ // Busca en otra cache
                    boolean estaEnOtraCache = cacheDatosNucleo1.existeBloque(bloqueMemoria);
                    if(estaEnOtraCache){
                        if(estadoEnOtraCache == 'M'){
                            cacheDatosLocal.cargarBloque(direccion, bloqueCache ,bloqueMemoria);
                            for(int i = 0; i < 32; i++){
                                cyclicBarrier.await();
                                relojNucleo0++;
                            }
                            cacheDatosLocal.escribirDato(direccion, instruccion[2]);
                            // Poner bloque en M
                            // Poner otro en I
                        }
                        else if(estadoEnOtraCache == 'C'){ // Se puede fusionar con el de arriba
                            // Poner estado otro bloque en I
                            cacheDatosLocal.cargarBloque(direccion, bloqueCache ,bloqueMemoria);
                            for(int i = 0; i < 32; i++){
                                cyclicBarrier.await();
                                relojNucleo0++;
                            }
                            cacheDatosLocal.escribirDato(direccion, instruccion[2]);
                            // POner bloque en M
                        }
                        if(RL == cacheDatosNucleo1.getRL()){
                            cacheDatosNucleo1.setRL(-1);
                        }
                    }
                    else{
                        cacheDatosLocal.cargarBloque(direccion, bloqueCache ,bloqueMemoria);
                        for(int i = 0; i < 32; i++){
                            cyclicBarrier.await();
                            relojNucleo0++;
                        }
                        cacheDatosLocal.escribirDato(direccion, instruccion[2]);
                        //Poner bloque en M
                    }
                }
                desbloquear();
                break;
            case 99:
                int resultado = Alu(2, registro[instruccion[1]], instruccion[2]);
                if(resultado == 0){
                    PC += (instruccion[3] * 4);
                }
                break;
            case 100:
                int resultado = Alu(2, registro[instruccion[1]], instruccion[2]);
                if(resultado != 0){
                    PC += (instruccion[3] * 4);
                }
                break;
            case 51:
                // Se calcula direccion de memoria y bloques
                int direccion = instruccion[2];
                int bloqueMemoria = direccion / 4;
                int palabra = direccion % 4;
                int bloqueCache = bloqueMemoria % 8;
                
                // Se bloquea cache local
                while(intentarBloqueo() == false){
                    cyclicBarrier.await();
                    relojNucleo0++;
                }

                // Se revisa si esta el bloque en cache
                boolean estaEnCache = cacheDatosLocal.existeBloque(bloqueCache, bloqueMemoria);

                if(estaEnCache){
                    // Si está, y no es "I", lee el dato
                    registro[instruccion[1]] = cacheDatosLocal.leerDato(direccion);
                }
                else{ // Si no esta en cache local

                    // Se busca si esta bloque en otra cache
                    boolean estaEnOtraCache = cacheDatosNucleo1.existeBloque(bloqueMemoria);
                        
                    if(!estaEnOtraCache){ // Si no esta en la otra, se trae el dato
                        int dato cacheDatosLocal.leerDato(direccion);
                        for(int i = 0; i < 32; i++){
                            cyclicBarrier.await();
                            relojNucleo0++;
                        }
                        // Guarda el dato en el registro 
                        registro[instruccion[1]] = dato;
                    }
                    else{ // Si esta el bloque en la otra cache(esta en "M" o "C")
                    // No se si hacer lo mismo en ambos casos("M","C")
                        if(estadoOtraCache == 'M'){
                            cacheDatosLocal.cargarBloque(direccion, bloqueCache ,bloqueMemoria);
                            for(int i = 0; i < 32; i++){
                                cyclicBarrier.await();
                                relojNucleo0++;
                            }
                            int dato = cacheDatosLocal.leerDato(direccion);
                            registro[instruccion[1]] = dato;
                        }
                        /*else if(estadoOtraCache == 'C'){
                            int dato = memoria.leerBloqueDatos(direccion, cacheDatosLocal[bloqueCache]);
                            for(int i = 0; i < 32; i++){
                                //cyclicBarrier.await();
                            }
                            registro[instruccion[1]] = dato;
                        }*/
                    }
                }
                this.RL = instruccion[2];
                // Se desbloquean los 3 recursos
                desbloquear();
                break;
            case 52:
                int direccion = instruccion[1];
                int bloqueMemoria = direccion / 4;
                int palabra = direccion % 4;
                int bloqueCache = bloqueMemoria % 8;

                // Se bloquea cache local
                while(intentarBloqueo() == false){
                    cyclicBarrier.await();
                    relojNucleo0++;
                }

                if(instruccion[1] == this.RL){
                    // Se revisa si esta bloque en cache
                    boolean estaEnCache = cacheDatosLocal.existeBloque(bloqueCache, bloqueMemoria);
                    
                    if(estaEnCache){
                        if(estado == 'M'){
                            // Se escribe dato en cache
                            cacheDatosLocal.escribirDato(direccion, instruccion[2]);
                        }
                        else if(estado == 'C'){
                            boolean estaEnOtraCache = cacheDatosNucleo1.existeBloque(bloqueMemoria);
                            if(estaEnOtraCache){
                                //Poner estado otra cache en I
                            }
                            cacheDatosLocal.escribirDato(direccion, instruccion[2]);
                            // Poner estado en "M"
                        }
                    }
                    else{ // Busca en otra cache
                        boolean estaEnOtraCache = cacheDatosNucleo1.existeBloque(bloqueMemoria);
                        if(estaEnOtraCache){
                            if(estadoEnOtraCache == 'M'){
                                cacheDatosLocal.cargarBloque(direccion, bloqueCache ,bloqueMemoria);
                                for(int i = 0; i < 32; i++){
                                    cyclicBarrier.await();
                                    relojNucleo0++;
                                }
                                cacheDatosLocal.escribirDato(direccion, instruccion[2]);
                                // Poner bloque en M
                                // Poner otro en I
                            }
                            else if(estadoEnOtraCache == 'C'){ // Se puede fusionar con el de arriba
                                // Poner estado otro bloque en I
                                cacheDatosLocal.cargarBloque(direccion, bloqueCache ,bloqueMemoria);
                                for(int i = 0; i < 32; i++){
                                    cyclicBarrier.await();
                                    relojNucleo0++;
                                }
                                cacheDatosLocal.escribirDato(direccion, instruccion[2]);
                                // POner bloque en M
                            }
                            if(RL == cacheDatosNucleo1.getRL()){
                                cacheDatosNucleo1.setRL(-1);
                            }
                        }
                        else{
                            cacheDatosLocal.cargarBloque(direccion, bloqueCache ,bloqueMemoria);
                            for(int i = 0; i < 32; i++){
                                cyclicBarrier.await();
                                relojNucleo0++;
                            }
                            cacheDatosLocal.escribirDato(direccion, instruccion[2]);
                            //Poner bloque en M
                        }
                    }
                }
                else{
                    registro[instruccion[2]] = 0;
                }
                desbloquear();
                break;
            case 111:
                registro[instruccion[1]] = PC;
                PC = PC + instruccion[3];
                break;
            case 103:
                registro[instruccion[1]] = PC;
                PC = instruccion[2] + instruccion[3];
                break;
            case 999:
                // Se guarda pcb en lista de procesos terminados
                siguienteHilillo(true,this.pcb);
                break;
        }
        quantumHililloActual--;
        if(instruccion[0] != 5 && instruccion[0] != 37 && instruccion[0] != 51 && instruccion[0] != 52){
            cyclicBarrier.await();
            relojNucleo0++;
        }
        if(instruccion[0] != 111 && instruccion[0] != 103){ // Si es un jump no hacer esto
            PC += 4;
        }
    }

    public void run(){
        // Este es la función que se llame con el nucleo0.start()
    }
}
