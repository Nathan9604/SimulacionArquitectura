import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.BrokenBarrierException;

public class Nucleo extends Thread {
    private int registro[];
    private int quantumTotal;
    private int quantumHililloActual;
    private CacheDatosC cacheDatosLocal;
    private CacheDatosD cacheDatosNucleo0;
    private int PC;
    private int RL;
    private String idHililloActual;
    private Planificador planificador;
    private Pcb pcb;
    private CacheInstrucciones cacheInstrucciones;
    private ReentrantLock lockDatosCache0;
    private ReentrantLock lockDatosCache1;
    private ReentrantLock lockMemoriaDatos;
    private int instruccionActual[] = {83,1,2,3};
    private int relojNucleo1;
    private CyclicBarrier barrera;

    public Nucleo(CacheInstrucciones instrucciones, CacheDatosC local, CacheDatosD cacheDatosNucleo0, int quantum, Planificador planificador,
                   ReentrantLock lockDatosCache0, ReentrantLock lockDatosCache1, ReentrantLock lockMemoriaDatos, CyclicBarrier barrera){
        this.cacheDatosLocal = local;
        this.cacheDatosNucleo0 = cacheDatosNucleo0;
        this.cacheInstrucciones = instrucciones;
        registro = new int[32];
        this.planificador = planificador;
        this.lockDatosCache0 = lockDatosCache0;
        this.lockDatosCache1 = lockDatosCache1;
        this.lockMemoriaDatos = lockMemoriaDatos;
        relojNucleo1 = 0;
        this.quantumTotal = quantum;
        this.barrera = barrera;
    }

    private void copiarPcbAContextoActual(Pcb pcb){
        this.registro = pcb.getRegistro();
        this.idHililloActual = pcb.getId();
        this.PC = pcb.getPc();
        this.RL = -1;
        quantumHililloActual = quantumTotal;
    }

    private void obtenerSiguienteInstruccion(){
        int direccion = PC;
        int bloqueMemoria = direccion / 16;
        int palabra = direccion % 16;
        int bloqueCache = bloqueMemoria % 8;
        if(cacheInstrucciones.estaEnCache(bloqueMemoria)){
            cacheInstrucciones.leerInstruccion(direccion, instruccionActual);
        }
        else{
            cacheInstrucciones.leerInstruccion(direccion, instruccionActual);
            /*for(int i = 0; i < 32; i++){
                try {
                    barrera.await();
                } catch (InterruptedException ex) {
                    return;
                } catch (BrokenBarrierException ex) {
                    return;
                }
                relojNucleo1++;
            }*/
        }
    }

    // Guarda el contexto del hilo actual y carga el contexto del siguiente hilo
    private void siguienteHilillo(boolean terminado, Pcb pcb){
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
                break;
            case 4:
                resultado = operando1 / operando2;
                break;
        }
        return resultado;
    }

    private boolean intentarBloqueo(){
        boolean respuesta = true;
        if(lockDatosCache0.tryLock() == false){
            respuesta = false;
        }
        if(lockDatosCache1.tryLock() == false){
            respuesta = false;
        }
        if(lockMemoriaDatos.tryLock() == false){
            respuesta = false;
        }
        return respuesta;
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
                    try {
                        barrera.await();
                    } catch (InterruptedException ex) {
                        return;
                    } catch (BrokenBarrierException ex) {
                        return;
                    }
                    relojNucleo1++;
                }

                // Se revisa si esta el bloque en cache
                boolean estaEnCache = cacheDatosLocal.existeBloque(bloqueMemoria);

                if(estaEnCache){
                    // Si está, y no es "I", lee el dato
                    registro[instruccion[1]] = cacheDatosLocal.leerDato(direccion);
                }
                else{ // Si no esta en cache local

                    // Se busca si esta bloque en otra cache
                    boolean estaEnOtraCache = cacheDatosNucleo0.existeBloque(bloqueCache, bloqueMemoria);

                    if(!estaEnOtraCache){ // Si no esta en la otra, se trae el dato
                        int dato = cacheDatosLocal.leerDato(direccion);
                        for(int i = 0; i < 32; i++){
                            try {
                                barrera.await();
                            } catch (InterruptedException ex) {
                                return;
                            } catch (BrokenBarrierException ex) {
                                return;
                            }
                            relojNucleo1++;
                        }
                        // Guarda el dato en el registro 
                        registro[instruccion[1]] = dato;
                    }
                    else{ // Si esta el bloque en la otra cache(esta en "M" o "C")
                        // No se si hacer lo mismo en ambos casos("M","C")
                        char estadoOtraCache = cacheDatosNucleo0.obtenerBandera(bloqueCache);
                        if(true){
                            cacheDatosLocal.leerDato(direccion);
                            for(int i = 0; i < 32; i++){
                                try {
                                    barrera.await();
                                } catch (InterruptedException ex) {
                                    return;
                                } catch (BrokenBarrierException ex) {
                                    return;
                                }
                                relojNucleo1++;
                            }
                            int dato = cacheDatosLocal.leerDato(direccion);
                            registro[instruccion[1]] = dato;
                        }
                    }
                }
                // Se desbloquean los 3 recursos
                desbloquear();
                break;
            case 37:
                direccion = Alu(1, instruccion[1], instruccion[3]);
                bloqueMemoria = direccion / 4;
                palabra = direccion % 4;
                bloqueCache = bloqueMemoria % 8;

                // Se bloquea cache local
                while(intentarBloqueo() == false){
                    try {
                        barrera.await();
                    } catch (InterruptedException ex) {
                        return;
                    } catch (BrokenBarrierException ex) {
                        return;
                    }
                    relojNucleo1++;
                }
                // Se revisa si esta bloque en cache
                estaEnCache = cacheDatosLocal.existeBloque(bloqueMemoria);
                char estado = cacheDatosLocal.obtenerBandera(bloqueCache);
                if(estaEnCache){
                    if(estado == 'M'){
                        // Se escribe dato en cache
                        cacheDatosLocal.escribirDato(direccion, instruccion[2]);
                    }
                    else if(estado == 'C'){
                        boolean estaEnOtraCache = cacheDatosNucleo0.existeBloque(bloqueCache,bloqueMemoria);
                        if(estaEnOtraCache){
                            cacheDatosNucleo0.cambiarBandera(bloqueCache, bloqueMemoria, 'I');
                        }
                        cacheDatosLocal.escribirDato(direccion, instruccion[2]);
                        cacheDatosLocal.cambiarBandera(bloqueCache, 'M');
                    }
                }
                else{ // Busca en otra cache
                    boolean estaEnOtraCache = cacheDatosNucleo0.existeBloque(bloqueCache,bloqueMemoria);
                    if(estaEnOtraCache){
                        char estadoEnOtraCache = cacheDatosNucleo0.obtenerBandera(bloqueCache);
                        if(estadoEnOtraCache == 'M'){
                            cacheDatosLocal.leerDato(direccion);
                            for(int i = 0; i < 32; i++){
                                try {
                                    barrera.await();
                                } catch (InterruptedException ex) {
                                    return;
                                } catch (BrokenBarrierException ex) {
                                    return;
                                }
                                relojNucleo1++;
                            }
                            cacheDatosLocal.escribirDato(direccion, instruccion[2]);
                            cacheDatosLocal.cambiarBandera(bloqueCache, 'M');
                            cacheDatosNucleo0.cambiarBandera(bloqueCache, bloqueMemoria, 'I');
                        }
                        else if(estadoEnOtraCache == 'C'){ // Se puede fusionar con el de arriba
                            cacheDatosNucleo0.cambiarBandera(bloqueCache, bloqueMemoria, 'I');
                            cacheDatosLocal.leerDato(direccion);
                            for(int i = 0; i < 32; i++){
                                try {
                                    barrera.await();
                                } catch (InterruptedException ex) {
                                    return;
                                } catch (BrokenBarrierException ex) {
                                    return;
                                }
                                relojNucleo1++;
                            }
                            cacheDatosLocal.escribirDato(direccion, instruccion[2]);
                            cacheDatosLocal.cambiarBandera(bloqueCache, 'M');
                        }
                        if(RL == cacheDatosNucleo0.getRl()){
                            cacheDatosNucleo0.setRl(-1);
                        }
                    }
                    else{
                        cacheDatosLocal.leerDato(direccion);
                        for(int i = 0; i < 32; i++){
                            try {
                                barrera.await();
                            } catch (InterruptedException ex) {
                                return;
                            } catch (BrokenBarrierException ex) {
                                return;
                            }
                            relojNucleo1++;
                        }
                        cacheDatosLocal.escribirDato(direccion, instruccion[2]);
                        // Se pone en modificado el bloque
                        cacheDatosLocal.cambiarBandera(bloqueCache, 'M');
                    }
                }
                desbloquear();
                break;
            case 99:
                int resultado = Alu(2, registro[instruccion[1]], instruccion[2]);
                if(resultado == 0){
                    PC += (instruccion[3] * 4);
                }
                else{
                    PC += 4;
                }
                break;
            case 100:
                resultado = Alu(2, registro[instruccion[1]], instruccion[2]);
                if(resultado != 0){
                    PC += (instruccion[3] * 4);
                }
                else{
                    PC += 4;
                }
                break;
            case 51:
                // Se calcula direccion de memoria y bloques
                direccion = instruccion[2];
                bloqueMemoria = direccion / 4;
                palabra = direccion % 4;
                bloqueCache = bloqueMemoria % 8;

                // Se bloquea cache local
                while(intentarBloqueo() == false){
                    try {
                        barrera.await();
                    } catch (InterruptedException ex) {
                        return;
                    } catch (BrokenBarrierException ex) {
                        return;
                    }
                    relojNucleo1++;
                }

                // Se revisa si esta el bloque en cache
                estaEnCache = cacheDatosLocal.existeBloque(bloqueMemoria);

                if(estaEnCache){
                    // Si está, y no es "I", lee el dato
                    registro[instruccion[1]] = cacheDatosLocal.leerDato(direccion);
                }
                else{ // Si no esta en cache local

                    // Se busca si esta bloque en otra cache
                    boolean estaEnOtraCache = cacheDatosNucleo0.existeBloque(bloqueCache,bloqueMemoria);

                    if(!estaEnOtraCache){ // Si no esta en la otra, se trae el dato
                        int dato = cacheDatosLocal.leerDato(direccion);
                        for(int i = 0; i < 32; i++){
                            try {
                                barrera.await();
                            } catch (InterruptedException ex) {
                                return;
                            } catch (BrokenBarrierException ex) {
                                return;
                            }
                            relojNucleo1++;
                        }
                        // Guarda el dato en el registro 
                        registro[instruccion[1]] = dato;
                    }
                    else{ // Si esta el bloque en la otra cache(esta en "M" o "C")
                        // No se si hacer lo mismo en ambos casos("M","C")
                        char estadoOtraCache = cacheDatosNucleo0.obtenerBandera(bloqueCache);
                        if(true){
                            cacheDatosLocal.leerDato(direccion);
                            for(int i = 0; i < 32; i++){
                                try {
                                    barrera.await();
                                } catch (InterruptedException ex) {
                                    return;
                                } catch (BrokenBarrierException ex) {
                                    return;
                                }
                                relojNucleo1++;
                            }
                            int dato = cacheDatosLocal.leerDato(direccion);
                            registro[instruccion[1]] = dato;
                        }
                    }
                }
                cacheDatosLocal.setRl(instruccion[2]);
                // Se desbloquean los 3 recursos
                desbloquear();
                break;
            case 52:
                direccion = instruccion[1];
                bloqueMemoria = direccion / 4;
                palabra = direccion % 4;
                bloqueCache = bloqueMemoria % 8;

                // Se bloquea cache local
                while(intentarBloqueo() == false){
                    try {
                        barrera.await();
                    } catch (InterruptedException ex) {
                        return;
                    } catch (BrokenBarrierException ex) {
                        return;
                    }
                    relojNucleo1++;
                }

                if(instruccion[1] == this.RL){
                    // Se revisa si esta bloque en cache
                    estaEnCache = cacheDatosLocal.existeBloque(bloqueMemoria);

                    if(estaEnCache){
                        estado = cacheDatosLocal.obtenerBandera(bloqueCache);
                        if(estado == 'M'){
                            // Se escribe dato en cache
                            cacheDatosLocal.escribirDato(direccion, instruccion[2]);
                        }
                        else if(estado == 'C'){
                            boolean estaEnOtraCache = cacheDatosNucleo0.existeBloque(bloqueCache,bloqueMemoria);
                            if(estaEnOtraCache){
                                cacheDatosNucleo0.cambiarBandera(bloqueCache, bloqueMemoria,'I');
                            }
                            cacheDatosLocal.escribirDato(direccion, instruccion[2]);
                            cacheDatosLocal.cambiarBandera(bloqueCache, 'M');
                        }
                    }
                    else{ // Busca en otra cache
                        boolean estaEnOtraCache = cacheDatosNucleo0.existeBloque(bloqueCache,bloqueMemoria);
                        if(estaEnOtraCache){
                            char estadoEnOtraCache = cacheDatosNucleo0.obtenerBandera(bloqueCache);
                            if(estadoEnOtraCache == 'M'){
                                cacheDatosLocal.leerDato(direccion);
                                for(int i = 0; i < 32; i++){
                                    try {
                                        barrera.await();
                                    } catch (InterruptedException ex) {
                                        return;
                                    } catch (BrokenBarrierException ex) {
                                        return;
                                    }
                                    relojNucleo1++;
                                }
                                cacheDatosLocal.escribirDato(direccion, instruccion[2]);
                                // Se cambian banderas
                                cacheDatosLocal.cambiarBandera(bloqueCache, 'M');
                                cacheDatosNucleo0.cambiarBandera(bloqueCache, bloqueMemoria,'I');
                            }
                            else if(estadoEnOtraCache == 'C'){ // Se puede fusionar con el de arriba
                                cacheDatosNucleo0.cambiarBandera(bloqueCache, bloqueMemoria,'I');
                                cacheDatosLocal.leerDato(direccion);
                                for(int i = 0; i < 32; i++){
                                    try {
                                        barrera.await();
                                    } catch (InterruptedException ex) {
                                        return;
                                    } catch (BrokenBarrierException ex) {
                                        return;
                                    }
                                    relojNucleo1++;
                                }
                                cacheDatosLocal.escribirDato(direccion, instruccion[2]);
                                cacheDatosLocal.cambiarBandera(bloqueCache, 'M');
                            }
                            if(RL == cacheDatosNucleo0.getRl()){
                                cacheDatosNucleo0.setRl(-1);
                            }
                        }
                        else{
                            cacheDatosLocal.leerDato(direccion);
                            for(int i = 0; i < 32; i++){
                                try {
                                    barrera.await();
                                } catch (InterruptedException ex) {
                                    return;
                                } catch (BrokenBarrierException ex) {
                                    return;
                                }
                                relojNucleo1++;
                            }
                            cacheDatosLocal.escribirDato(direccion, instruccion[2]);
                            // Se pone bloque en Modificado
                            cacheDatosLocal.cambiarBandera(bloqueCache, 'M');
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
            try {
                barrera.await();
            } catch (InterruptedException ex) {
                return;
            } catch (BrokenBarrierException ex) {
                return;
            }
            relojNucleo1++;
        }
        if(instruccion[0] != 111 && instruccion[0] != 103 && instruccion[0] != 99 && instruccion[0] != 100){ // Si es un jump no hacer esto
            PC += 4;
        }
    }

    public void run(){
        while(true){
            obtenerSiguienteInstruccion();
            decodificador(instruccionActual);
        }
    }
}