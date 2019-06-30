public class Nucleo extends Thread {
    /*private int registro[];
    private int quantumTotal;
    private int quantumHililloActual;
    private CacheDatosC cacheDatosLocal;
    private CacheDatosD cacheDatosNucleo1;
    private int PC;
    private int RL;
    private int idHililloActual;
    private Planificador planificador;
    private Pcb pcb;

    public Nucleo(cacheDatosC local, chacheDatosD cacheDatosNucleo1, int quantum, Planificador planificador){
        this.cacheDatosLocal = local;
        this.cacheDatosNucleo1 = cacheDatosNucleo1;
        registro = new int[32];
        this.planificador = planificador;
    }

    private void copiarPcbAContextoActual(Pcb pcb){
        this.registro = pcb.getRegistro();
        this.idHililloActual = pcb.getId();
        this.PC = pcb.getPc();
        this.RL = -1;
        quantumHililloActual = quantumTotal;
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
                int direccion = Alu(1, instruccion[2], instruccion[3]);
                int bloqueMemoria = direccion / 4;
                int palabra = direccion % 4;
                int bloqueCache = bloqueMemoria / 8;
                //lockDatosCache1.lock();
                boolean estaEnCache = cacheDatosLocal.estaEnCache(bloqueMemoria);
                char estado;
                // Metodo para ver estado del bloque.
                if(estaEnCache && estado != 'I'){
                    //registro[instruccion[1]] = cacheDatosLocal.leerDato();
                    //lockDatosCache1.unlock();
                }
                else{
                    // revisar bloque victima
                    // si estado = M
                    if(lockDatosCache2.trylock() == false){
                        //lockDatosCache1.unlock();

                    }
                    boolean estaEnOtraCache = cacheDatosNucleo1.estaEnCache(bloqueMemoria);
                    char estadoOtraCache;
                    if(estaEnCache){
                        estadoOtraCache = cacheDatosNucleo1.estadoBloque(bloqueCache);
                    }
                    else{
                        estadoOtraCache = 'N';
                    }
                    
                    if(!estaEnOtraCache || estadoOtraCache == 'I'){
                        //lockDatosCache2.unlock();
                        //lockMemoriaDatos.lock();
                        memoria.leerBloqueDatos(direccion, cacheDatosLocal[bloqueCache]);
                        for(int i = 0; i < 32; i++){
                            //cyclicBarrier.await();
                            relojNucleo0++;
                        }
                        //lockMemoriaDatos.unlock();
                        int dato = cacheDatosLocal.leerDato();
                        registro[instruccion[1]] = dato;
                        //lockDatosCache1.unlock();
                    }
                    else{
                        if(estadoOtraCache == 'M'){
                            cacheDatosNucleo1.ponerEstado('C', bloqueCache);
                            cacheDatosLocal.ponerEstado('C', bloqueCache);
                            cacheDatosLocal[bloqueCache] = cacheDatosNucleo1[bloqueCache];
                            //lockMemoriaDatos.lock();
                            memoria.escribirBloqueDatos(direccion, cacheDatosNucleo1[bloqueCache]);
                            for(int i = 0; i < 32; i++){
                                //cyclicBarrier.await();
                                relojNucleo0++;
                            }
                            //lockMemoriaDatos.unlock();
                            //lockDatosCache1.unlock();
                            //lockDatosCache2.unlock();
                        }
                        else if(estadoOtraCache == 'C'){
                            //lockDatosCache2.unlock();
                            //lockMemoriaDatos.lock();
                            int dato = memoria.leerBloqueDatos(direccion, cacheDatosLocal[bloqueCache]);
                            for(int i = 0; i < 32; i++){
                                //cyclicBarrier.await();
                                relojNucleo0++;
                            }
                            //lockMemoriaDatos.unlock();
                            registro[instruccion[1]] = dato;
                            //lockDatosCache1.unlock();
                        }
                    }
                }
                break;
            case 37:
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
                break;
            case 52:
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
            //cyclicBarrier.await();
            //relojNucleo0++;
        }
        PC += 4;
    }

    public void run(){
        // Este es la función que se llame con el nucleo.start()
    }*/
}
