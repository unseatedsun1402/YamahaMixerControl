// const version = "20251007";
const IS_DEBUG = false;

function sendMessage(element) {
  clearTimeout(element._debounce);
  element._debounce = setTimeout(() => {
    const msb = parseInt(element.dataset.msb, 10);
    const lsb = parseInt(element.dataset.lsb, 10);
    const value = parseInt(element.value, 10);

    // Send NRPN message
    const message = JSON.stringify({
      id: element.id,
      param: 0,
      value,
      msb,
      lsb
    });

    saveFaderState(msb, lsb, value);
    Client.sendMessage(message);
    localStorage.setItem("lastSync", Date.now());
      if (IS_DEBUG){console.info("Sent message "+message)}
    // Update label immediately
    const label = document.querySelector(`.fader-value[data-msb="${msb}"][data-lsb="${lsb}"]`);
    if (label) {
        const db = faderDbMap[value] || "-∞";
        label.textContent = `${db} dB`;
        if (IS_DEBUG){console.debug("Updating fader "+ msb + " " + lsb + " to " +  db)}
        label.classList.add("updated");
        setTimeout(() => label.classList.remove("updated"), 150);
    }
    else{
        console.warn("Cannot find fader " + msb + " " + lsb)
    }
  }, 12);
}

function restoreFaderState(){   //inject any preserved values from localstorage
    document.querySelectorAll(".fader").forEach(fader => {
    const msb = fader.dataset.msb;
    const lsb = fader.dataset.lsb;
    const key = `fader-${msb}-${lsb}`;
    const savedValue = localStorage.getItem(key);

    if (savedValue !== null) {
        fader.value = savedValue;
        sendMessage(fader); // reapply value and update label

        fader.classList.add("restored");
        setTimeout(() => fader.classList.remove("restored"), 500);
    }
    });
}

// Builds the control elements on a page
async function buildpage() {
  const body = document.body;
  const fadermsb = body.dataset.fadermsb || "0";
  const faderlsb = body.dataset.faderlsb || "0";

  const URL = `http://${window.location.host}/MidiControl/buildpage?coarse=${fadermsb}&fine=${faderlsb}`;
  try {
    const response = await fetch(URL);
    const html = await response.text();

    if (html) {
      document.getElementById("ch-levels").innerHTML = html;
      console.log("Fader HTML injected:", html);

      const elements = document.getElementsByClassName("fader");
      for (let element of elements) {
        console.log("Fader ID:", element.id);
      }
      injectOutputFaders()

      restoreFaderState() // automatic rehydration
      document.getElementById("restoreState").disabled = false; //enable the restore button now the page has loaded
      document.getElementById("restoreState").addEventListener("click", restoreFaderState); // start listening to the button
      console.log("Restored saved states")
    }
  } catch (err) {
    console.error("Error loading fader layout:", err);
  }
}
     
    
async function selectDevice(){
    const URL='http://'+window.location.host+'/MidiControl/devices';
    const Res= fetch(URL);
    const response= await Res;
    const json= await response.text();
    if(!(json==null)){
        document.getElementById("midiDevice").innerHTML = json;
    }
}

async function selectInputDevice(){
    const URL='http://'+window.location.host+'/MidiControl/GetInputDevices';
    const Res= fetch(URL);
    const response= await Res;
    const html= await response.text();
    if(!(html==null)){
        document.getElementById("midiInputDevice").innerHTML = html;
    }
}

function doSet(){
    var selector = document.getElementById("midi-select");
    console.trace("Output device selected %s", selector.value);
    if (selector !== null) {
        fetch("setmidioutputdevice?set=" + selector.value);
        var index = parseInt(selector.value, 10);
        document.getElementById("selected").innerHTML =
            "<h2>" + selector.options[index].innerHTML + " in use</h2>";
    }
}

function doSetInput(){
    var selector = document.getElementById("midi-input-select");
    console.trace("Input device selected %s", selector.value);
    if (selector !== null) {
        fetch("setmidiinputdevice?set=" + selector.value);
        var index = parseInt(selector.value, 10);
        document.getElementById("selectedInput").innerHTML =
            "<h2>" + selector.options[index].innerHTML + " in use</h2>";
    }
}

// Yamaha M7CL style fader law lookup
const faderDbMap = [
"−∞", // 0
"-134.0","-129.2","-124.4","-119.6","-114.8","-110.0","-105.2","-100.4","-95.6", // 1–9
"-90.0","-88.1","-86.2","-84.3","-82.4","-80.5","-78.6","-76.7","-74.8","-72.9", // 10–19
"-70.0","-69.0","-68.0","-67.0","-66.0","-65.0","-64.0","-63.0","-62.0","-61.0", // 20–29
"-60.0","-58.1","-56.2","-54.3","-52.4","-50.5","-48.6","-46.7","-44.8","-42.9", // 30–39
"-40.0","-39.1","-38.2","-37.3","-36.4","-35.5","-34.6","-33.7","-32.8","-31.9", // 40–49
"-30.0","-29.1","-28.2","-27.3","-26.4","-25.5","-24.6","-23.7","-22.8","-21.9", // 50–59
"-20.0","-19.6","-19.1","-18.7","-18.2","-17.8","-17.3","-16.9","-16.4","-15.9", // 60–69
"-15.0","-14.5","-14.0","-13.5","-13.0","-12.5","-12.0","-11.5","-11.0","-10.5", // 70–79
"-10.0","-9.5","-9.0","-8.5","-8.0","-7.5","-7.0","-6.5","-6.0","-5.5",          // 80–89
"-5.0","-4.5","-4.0","-3.5","-3.0","-2.5","-2.0","-1.5","-1.0","-0.5","0.0",     // 90–100
"+0.5","+1.0","+1.5","+2.0","+2.5","+3.0","+3.5","+4.0","+4.5",                 // 101–109
"+5.0","+5.3","+5.6","+5.9","+6.2","+6.5","+6.8","+7.1","+7.4","+7.7",           // 110–119
"+8.0","+8.3","+8.6","+8.9","+9.2","+9.5","+9.8","+10.0"                         // 120–127
];

function updateFader(channelIndex, step) {
    const faderId = `ch${channelIndex}`;
    const valueId = `f${channelIndex}-value`;

    const fader = document.getElementById(faderId);
    const valueLabel = document.getElementById(valueId);

    // Lookup dB label
    const db = faderDbMap[step] || "?";
    if (valueLabel) {
        valueLabel.innerText = db + " dB";
    }

    if (fader) {
        fader.value = step;
        fader.classList.add("updated");
        setTimeout(() => fader.classList.remove("updated"), 300);
    }
}

document.querySelectorAll("input[type=range].fader").forEach(fader => {
    fader.addEventListener("input", e => {
        const channelIndex = parseInt(e.target.dataset.channelIndex, 10);
        const step = parseInt(e.target.value, 10);

        // Update label immediately
        updateFader(channelIndex, step);

        // Send back to server
        const normValue = Math.round((step / 127) * 16383);
        socket.send(JSON.stringify({
            type: "nrpnUpdate",
            channelIndex,
            value: normValue
        }));
    });
});
    
function handleNrpnUpdate(update) {
  if (IS_DEBUG){console.debug("handleNrpnUpdate called with:", update);}
  const { msb, lsb, value } = update;
  const fader = document.querySelector(`input[data-msb="${msb}"][data-lsb="${lsb}"]`);
  const valueLabel = document.querySelector(`.fader-value[data-msb="${msb}"][data-lsb="${lsb}"]`);

  if (!fader) {
    console.warn(`No fader found for NRPN msb:${msb} lsb:${lsb}`);
    return;
  }

  const step = Math.round((value / 16383) * 127);
  const db = faderDbMap[step];

  fader.value = step;

  if (valueLabel) {
    valueLabel.innerText = db === "-∞" ? db : `${db} dB`;
    if (IS_DEBUG){console.debug("Updating fader " + msb + " " + lsb + " to " + db);}
    valueLabel.classList.add("updated");
    setTimeout(() => valueLabel.classList.remove("updated"), 150);
  } else {
    console.warn(`No value label found for NRPN msb:${msb} lsb:${lsb}`);
  }

  fader.classList.add("updated");
  setTimeout(() => fader.classList.remove("updated"), 300);

  saveFaderState(msb, lsb, step); // Save scaled value
  if (IS_DEBUG){console.debug(`Updated fader ${fader.id} to ${db} dB`);}
}

function saveFaderState(msb, lsb, value) {
  const key = `fader-${msb}-${lsb}`;
  localStorage.setItem(key, value);
}

function handleOutputFaderUpdate(update) {
  if (IS_DEBUG){console.debug("Output Update called with:", update);}
  const { msb, lsb, value } = update;
  const fader = document.querySelector(`input[data-msb="${msb}"][data-lsb="${lsb}"]`);
  const valueLabel = document.querySelector(`.fader-value[data-msb="${msb}"][data-lsb="${lsb}"]`);

  if (!fader) {
    console.warn(`No fader found for NRPN msb:${msb} lsb:${lsb}`);
    return;
  }

  const step = Math.round((value / 16383) * 127);
  const db = faderDbMap[step];

  fader.value = step;

  if (valueLabel) {
    valueLabel.innerText = db === "-∞" ? db : `${db} dB`;
    if (IS_DEBUG){console.debug("Updating fader " + msb + " " + lsb + " to " + db);}
    valueLabel.classList.add("updated");
    setTimeout(() => valueLabel.classList.remove("updated"), 150);
  } else {
    console.warn(`No value label found for NRPN msb:${msb} lsb:${lsb}`);
  }

  fader.classList.add("updated");
  setTimeout(() => fader.classList.remove("updated"), 300);

  saveFaderState(msb, lsb, step); // Save scaled value
  console.log(`Updated fader ${fader.id} to ${db} dB`);
}

function saveFaderState(msb, lsb, value) {
  const key = `fader-${msb}-${lsb}`;
  localStorage.setItem(key, value);
}
