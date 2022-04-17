//import * as schedule from "node-schedule";

let thisInn;
let showFlag = false;

//const job = schedule.scheduleJob('0 1 * * * *', checkUpdating());

function checkUpdating() {
    var xhr = new XMLHttpRequest();
    var url = "/shifts/isUpdate";
    xhr.open("GET", url, true);
    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.send();
    xhr.onreadystatechange = function () {

        if (xhr.readyState === 4 && xhr.status === 200) {
            /*[0] - kktService.isErr(),[1] - kktService.isUpdating(),[2] - receiptService.isErr(),[3] - receiptService.isUpdating()*/
            let bools = JSON.parse(xhr.responseText);

            if (bools[1] === true || bools[3] === true) {
                document.getElementById('baseUpdateLoading').style.display = 'flex';
                document.getElementById("err").style.display = 'none';
                document.getElementById("accept").style.display = 'none';
            } else if (bools[1] === false && bools[3] === false) {
                document.getElementById('baseUpdateLoading').style.display = 'none';
                if (bools[0] === false && bools[2] === false) {
                    document.getElementById("accept").style.display = 'inline-block';
                    document.getElementById("err").style.display = 'none';
                } else {
                    document.getElementById("accept").style.display = 'none';
                    document.getElementById("err").style.display = 'inline-block';
                }
            }
        }
    }
}

function showKKTs(kktset, inn) {
    document.getElementById('main').innerHTML = '';
    document.getElementById('header').innerHTML = '';
    let check = kktset;
    thisInn = inn;

    kktset.sort((a, b) => a.fiscalAddress.localeCompare(b.fiscalAddress));

    //Вывод вспомогательного header'а для выбора всех касс разом и поиску по адресу касс
    document.getElementById('main').innerHTML += "<header class=footerLike>" +
        "<input type=\"checkbox\" id='select-all'/><input type=\"text\" id=\"search\" onkeyup=\"searchEngine()\" placeholder=\"Поиск по адресу\"></header>";


    checkUpdating();


    //Вывод касс
    for (let i = check.length - 1; i >= 0; i--) {
        document.getElementById('main').innerHTML += "<div><a href='#' onclick ='showReceipts(" + JSON.stringify(check[i]) + ")'>Регистрационный номер кассы: " +
            check[i].kktRegNumber + "; Адрес: <span>" + check[i].fiscalAddress + "</span>; Дата последнего загруженного чека: " + moment(check[i].lastDocOnOfdDateTime).format('DD-MM-YYYY, HH:mm:ss') + "</a><input type=\"checkbox\" id=\"" + check[i].id + "\" name=kkt value=\"" + check[i].kktRegNumber + "\">" +
            "</div>";
    }


    //Вывод кнопок footer'a в элементе main, отвечающие за отчёты, удаление ИНН
    document.getElementById('main').innerHTML += "<footer style=\"width:100%;margin-right:1%;\">" +
        "<button class='button info' id='btnDay' name=post  onclick=document.getElementById('blurBackground').style.display='flex';" +
        "document.getElementById('day').style.display='flex';>Создать отчёт за сегодня</button>" +
        "<button class='button info' id='btnWeek' name=post style=\"margin-left:1%;\" onclick=document.getElementById('blurBackground').style.display='flex';" +
        "document.getElementById('week').style.display='flex';>Создать отчёт за неделю</button>" +
        "<button class='button info' id='btnMonth' name=post style=\"margin-left:1%;\" onclick=document.getElementById('blurBackground').style.display='flex';" +
        "document.getElementById('month').style.display='flex';>Создать отчёт за месяц</button>" +
        "<button class='button info' id='btnYear' name=post style=\"margin-left:1%;\" onclick=document.getElementById('blurBackground').style.display='flex';" +
        "document.getElementById('year').style.display='flex';>Создать отчёт за год</button>" +
        "<button class='button info' id='btnDeleteInn' name=post style=\"margin-left:1%;\" onclick=document.getElementById('blurBackground').style.display='flex';" +
        "document.getElementById('deleteInn').style.display='flex';>Удалить этот ИНН</button>" +
        "<button class='button info' id='btnPeriod' name=post style=\"margin-left:1%;\" onclick=document.getElementById('blurBackground').style.display='flex';" +
        "document.getElementById('period').style.display='flex';>Создать отчёт за определёный период</button></footer>";

    //Выбор всех касс
    document.getElementById('select-all').onclick = function () {
        var checkboxes = document.getElementById('main').querySelectorAll('input[type="checkbox"]');
        for (var checkbox of checkboxes) {
            checkbox.checked = this.checked;
        }
    }


    //Вывод кнопки обновления в header
    document.getElementById('header').innerHTML += "<div class='baseState'><div class=\"statusBaseState\"><img src=\"../error.png\" id=\"err\" title=\"При загрузке/обновлении базы произошла ошибка. Перезапустите обновление, либо пересоздайте базу.\" class=\"img\"><img src=\"../accept.png\" title=\"База загружена и актуальна\" id=\"accept\" class=\"img\"><section class=\"lds-dual-ring-status\" title=\"Идёт загрузка/обновление базы инн/ккт/чеков\" id=\"baseUpdateLoading\" style=\"display: none;\"></section></div><button class='button update' id='btnUpdate' name=post  onclick=document.getElementById('blurBackground').style.display='flex';document.getElementById('update').style.display='flex';>Обновить текущую базу данных</button></div>";

    // Чтобы не было 100500 касс, если много раз кликать по блоку ИНН
    if (showFlag === false) {
        document.getElementById('blurBackground').querySelectorAll('button[name="post"]').forEach(function (item) {
            item.addEventListener('click', function () {
                document.getElementById('blurBackground').querySelectorAll('.lds-dual-ring').forEach(el => el.style.display = 'none');
                document.getElementById('blurBackground').querySelectorAll("button[name='report']").forEach(el => el.style.display = 'flex');
                document.getElementById('blurBackground').querySelectorAll('p[id="response"]').forEach(el => el.style.display = 'none');
            })
        });

        //Отправляем кассы в функции отчётов
        document.getElementById('blurBackground').querySelectorAll('button[name="report"]').forEach(function (item) {
            item.addEventListener('click', function () {
                let checkboxes = document.getElementById('main').querySelectorAll('input[name="kkt"]:checked');
                let values = [];
                checkboxes.forEach((checkbox) => {
                    values.push(checkbox.value);
                });
                switch (item.id) {
                    case 'dayBtn':
                        createDayReport(values);
                        break
                    case 'weekBtn':
                        createWeekReport(values);
                        break
                    case 'monthBtn':
                        createMonthReport(values);
                        break
                    case 'yearBtn':
                        createYearReport(values);
                        break
                    case 'periodBtn':
                        let from = document.getElementById('period').querySelector('input[name="fromPeriod"]').value;
                        let to = document.getElementById('period').querySelector('input[name="toPeriod"]').value;
                        if (from !== '' && from !== null && to !== '' && to !== null) {
                            createPeriodReport(from, to, values);
                        }
                        break
                    case 'deleteInnBtn':
                        deleteInn(thisInn);
                        break
                }
            });
        })
        showFlag = true;
    }

}

function addUserButton() {
    let login = document.getElementById('userAdd').querySelector('input[name="login"]').value;
    let pass = document.getElementById('userAdd').querySelector('input[name="pass"]').value;
    if (login !== '' && login !== null && pass !== '' && pass !== null) {
        addUser(login, pass);
    }
}

function addInnButton() {
    let name = document.getElementById('addInn').querySelector('input[name="name"]').value;
    let inn = document.getElementById('addInn').querySelector('input[name="inn"]').value;
    if (name !== '' && name !== null && inn !== '' && inn !== null) {
        addInn(name, inn);
    }
}

function deleteThisInn() {
    deleteInn(thisInn);
}

function updateButton() {
    updateBase();
}

function closeBtn(name, loading, btn) {
    document.getElementById('blurBackground').style.display = 'none';
    document.getElementById(name).style.display = 'none';
    if (name !== 'receipts') {
        document.getElementById(btn).style.display = 'flex';
        document.getElementById(loading).style.display = 'none';
    } else {
        document.getElementById('receipts').removeChild(document.getElementById('receiptsTable'));
        document.getElementById('receipts').style.width = '50%';
        document.getElementById('receipts').style.height = '25%';
    }
}

function closeBtn2(name) {
    document.getElementById('blurBackground').querySelectorAll("button[name='close']").forEach(function (item) {
        item.addEventListener('click', function () {
            document.getElementById('blurBackground').querySelectorAll('button[name="report"]').forEach(el => el.style.display = 'flex');
            document.getElementById(name).removeChild(document.getElementById('response'));
        })
    });
}

function createDayReport(values) {
    document.getElementById('dayBtn').style.display = 'none';
    document.getElementById('dayLoading').style.display = 'flex';
    var to = moment().format();
    var from = moment(to).set('hour', 0).set('minute', 0).set('second', 0).format();
    var xhr = new XMLHttpRequest();
    var url = "/shifts/reports";
    xhr.open("POST", url, true);
    xhr.setRequestHeader("Content-Type", "application/json");

    var data = JSON.stringify({
        "from": from.toString().substr(0, from.toString().length - 6),
        "to": to.toString().substr(0, to.toString().length - 6), "kkts": values
    });
    xhr.send(data);
    xhr.onreadystatechange = function () {
        if (xhr.readyState === 4 && xhr.status === 200) {
            closeBtn2('day');
            document.getElementById('dayLoading').style.display = 'none';
            let p = document.createElement('p');
            p.id = 'response';
            document.getElementById('day').appendChild(p);
            let str = JSON.parse(xhr.responseText);
            document.getElementById('response').innerHTML += str[0];
            window.open('shifts/reports/' + str[1], '_blank').focus();
        }
    };
}

function createWeekReport(values) {
    document.getElementById('weekBtn').style.display = 'none';
    document.getElementById('weekLoading').style.display = 'flex';
    var to = moment().format();
    var from = moment(to).set('hour', 0).set('minute', 0).set('second', 0).subtract(1, 'weeks').format();
    var xhr = new XMLHttpRequest();
    var url = "/shifts/reports";
    xhr.open("POST", url, true);
    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.onreadystatechange = function () {
        if (xhr.readyState === 4 && xhr.status === 200) {
            closeBtn2('week');
            document.getElementById('weekLoading').style.display = 'none';
            let p = document.createElement('p');
            p.id = 'response';
            document.getElementById('week').appendChild(p);
            let str = JSON.parse(xhr.responseText);
            document.getElementById('response').innerHTML += str[0];
            window.open('shifts/reports/' + str[1], '_blank').focus();

        }
    };
    var data = JSON.stringify({
        "from": from.toString().substr(0, from.toString().length - 6),
        "to": to.toString().substr(0, to.toString().length - 6), "kkts": values
    });
    xhr.send(data);
}


function createMonthReport(values) {
    document.getElementById('monthBtn').style.display = 'none';
    document.getElementById('monthLoading').style.display = 'flex';
    var to = moment().format();
    var from = moment(to).set('hour', 0).set('minute', 0).set('second', 0).subtract(1, 'months').format();
    var xhr = new XMLHttpRequest();
    var url = "/shifts/reports";
    xhr.open("POST", url, true);
    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.onreadystatechange = function () {
        if (xhr.readyState === 4 && xhr.status === 200) {
            closeBtn2('month');
            document.getElementById('monthLoading').style.display = 'none';
            let p = document.createElement('p');
            p.id = 'response';
            document.getElementById('month').appendChild(p);
            let str = JSON.parse(xhr.responseText);
            document.getElementById('response').innerHTML += str[0];
            window.open('shifts/reports/' + str[1], '_blank').focus();
        }
    };
    var data = JSON.stringify({
        "from": from.toString().substr(0, from.toString().length - 6),
        "to": to.toString().substr(0, to.toString().length - 6), "kkts": values
    });
    xhr.send(data);

}

function createYearReport(values) {
    document.getElementById('yearBtn').style.display = 'none';
    document.getElementById('yearLoading').style.display = 'flex';
    var to = moment().format();
    var from = moment(to).set('hour', 0).set('minute', 0).set('second', 0).subtract(1, 'years').format();
    var xhr = new XMLHttpRequest();
    var url = "/shifts/reports";
    xhr.open("POST", url, true);
    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.onreadystatechange = function () {
        if (xhr.readyState === 4 && xhr.status === 200) {
            closeBtn2('year');
            document.getElementById('yearLoading').style.display = 'none';
            let p = document.createElement('p');
            p.id = 'response';
            document.getElementById('year').appendChild(p);
            let str = JSON.parse(xhr.responseText);
            document.getElementById('response').innerHTML += str[0];
            window.open('shifts/reports/' + str[1], '_blank').focus();
        }
    };
    var data = JSON.stringify({
        "from": from.toString().substr(0, from.toString().length - 6),
        "to": to.toString().substr(0, to.toString().length - 6), "kkts": values
    });
    xhr.send(data);

}

function deleteInn(inn) {
    document.getElementById('deleteInnBtn').style.display = 'none';
    document.getElementById('deleteInnLoading').style.display = 'flex';
    var xhr = new XMLHttpRequest();
    var url = "/shifts/deleteInn";
    xhr.open("POST", url, true);
    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.onreadystatechange = function () {
        if (xhr.readyState === 4 && xhr.status === 200) {
            document.getElementById('deleteInnLoading').style.display = 'none';
            let p = document.createElement('p');
            p.id = 'response';
            document.getElementById('deleteInn').appendChild(p);
            document.getElementById('response').innerHTML += xhr.responseText;
            document.getElementById('deleteInn').querySelector('button[name="close"]').onclick = function () {
                location.href = 'main';
            };
        }
    };
    var data = JSON.stringify({
        "inn": inn
    });
    xhr.send(data);
}

function createPeriodReport(from, to, values) {
    document.getElementById('periodBtn').style.display = 'none';
    document.getElementById('periodLoading').style.display = 'flex';
    var xhr = new XMLHttpRequest();
    var url = "/shifts/reports";
    xhr.open("POST", url, true);
    xhr.setRequestHeader("Content-Type", "application/json");
    var data = JSON.stringify({
        "from": from,
        "to": to, "kkts": values
    });
    xhr.send(data);
    xhr.onreadystatechange = function () {
        if (xhr.readyState === 4 && xhr.status === 200) {
            closeBtn2('period');
            document.getElementById('periodLoading').style.display = 'none';
            let p = document.createElement('p');
            p.id = 'response';
            document.getElementById('period').appendChild(p);
            let str = JSON.parse(xhr.responseText);
            document.getElementById('response').innerHTML += str[0];
            window.open('shifts/reports/' + str[1], '_blank').focus();
        }
    };
}

function addInn(name, inn) {
    document.getElementById('addInnBtn').style.display = 'none';
    document.getElementById('addInnLoading').style.display = 'flex';
    var xhr = new XMLHttpRequest();
    var url = "/shifts/addInn";
    xhr.open("POST", url, true);
    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.onreadystatechange = function () {
        if (xhr.readyState === 4 && xhr.status === 200) {
            document.getElementById('addInnLoading').style.display = 'none';
            let p = document.createElement('p');
            p.id = 'response';
            document.getElementById('addInn').appendChild(p);
            document.getElementById('response').innerHTML += xhr.responseText;
            document.getElementById('addInn').querySelector('button[name="close"]').onclick = function () {
                location.href = 'main';
            };
        }
        if (xhr.onreadystatechange === 4 && xhr.status === 403) {
            deleteInn(inn);
            addInn(name, inn);
        }
    };
    var data = JSON.stringify({
        "name": name.toString(),
        "inn": inn.toString()
    });
    xhr.send(data);
}

function addUser(login, pass) {
    document.getElementById('addUserBtn').style.display = 'none';
    document.getElementById('addUserLoading').style.display = 'flex';
    var xhr = new XMLHttpRequest();
    var url = "/shifts/signup";
    xhr.open("POST", url, true);
    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.onreadystatechange = function () {
        if (xhr.readyState === 4 && xhr.status === 200) {
            location.href = "main";
        } else {
            if (xhr.readyState === 4 && xhr.status !== 200) {
                closeBtn2('addUser');
                document.getElementById('addUserLoading').style.display = 'none';
                let p = document.createElement('p');
                p.id = 'response';
                document.getElementById('addUser').appendChild(p);
                document.getElementById('response').innerHTML += xhr.responseText;
            }
        }
    };
    var data = JSON.stringify({
        "Login": login.toString(),
        "Password": pass.toString()
    });
    xhr.send(data);
}

function updateBase() {
    document.getElementById('updateBtn').style.display = 'none';
    document.getElementById('updateLoading').style.display = 'flex';
    var xhr = new XMLHttpRequest();
    var url = "/shifts/update";
    xhr.open("GET", url, true);
    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.onreadystatechange = function () {
        if (xhr.readyState === 4 && xhr.status === 200) {
            document.getElementById('updateLoading').style.display = 'none';
            let p = document.createElement('p');
            p.id = 'response';
            document.getElementById('update').appendChild(p);
            document.getElementById('response').innerHTML += xhr.responseText;
            document.getElementById('update').querySelector('button[name="close"]').onclick = function () {
                location.href = 'main';
            };
        }
        if (xhr.readyState === 4 && xhr.status === 403) {
            updateBase();
        }
        if (xhr.readyState === 4 && xhr.status !== 200) {
            closeBtn2('update');
            document.getElementById('updateLoading').style.display = 'none';
            let p = document.createElement('p');
            p.id = 'response';
            document.getElementById('update').appendChild(p);
            document.getElementById('response').innerHTML += xhr.responseText;
        }
    };
    xhr.send();
}

function searchEngine() {
    var input, filter, main, divTag, span, txtValue;
    input = document.getElementById('search');
    main = document.getElementById('main');
    divTag = main.getElementsByTagName('div');

    for (let i = 0; i < divTag.length; i++) {
        span = divTag[i].getElementsByTagName("span")[0];
        txtValue = span.textContent || span.innerText;
        if (txtValue.toUpperCase().indexOf(input.value.toUpperCase()) > -1) {
            divTag[i].style.display = "block";
        } else {
            divTag[i].style.display = "none";
        }
    }
}

function showReceipts(kkt) {
    document.getElementById('blurBackground').style.display = 'flex';
    document.getElementById('receipts').style.display = 'flex';
    document.getElementById('receiptsBtn').replaceWith(document.getElementById('receiptsBtn').cloneNode(true));
    document.getElementById('blurBackground').querySelector('button[name="receiptsBtn"]').addEventListener('click', () => {
        let from = document.getElementById('receipts').querySelector('input[name="receiptsDate"]').value;
        if (from !== null && from !== '') {
            loadReceiptsByDateAndKkt(from, kkt.id);
        }
    })
}

function loadReceiptsByDateAndKkt(date, id) {
    let i = 0;
    if (document.getElementById('receipts').contains(document.getElementById('receiptsTable'))) {
        document.getElementById('receipts').removeChild(document.getElementById('receiptsTable'));
    }
    document.getElementById('receipts').style.width = '50%';
    document.getElementById('receipts').style.height = '25%';
    document.getElementById('receiptsLoading').style.display = 'flex';
    var xhr = new XMLHttpRequest();
    var url = "/shifts/receipts";
    xhr.open("POST", url, true);
    xhr.setRequestHeader("Content-Type", "application/json");
    var data = JSON.stringify({
        "date": date.toString(),
        "id": id.toString()
    });
    xhr.onreadystatechange = function () {
        if (xhr.readyState === 4 && xhr.status === 200) {
            let receipts = JSON.parse(xhr.responseText);
            closeBtn2('receipts');
            document.getElementById('receiptsLoading').style.display = 'none';
            let table = document.createElement('table');
            table.id = 'receiptsTable';

            var col = [];
            for (i = 0; i < receipts.length; i++) {
                for (var key in receipts[i]) {
                    if (col.indexOf(key) === -1) {
                        col.push(key);
                    }
                }
            }

            var tr = table.insertRow(-1);

            for (i = 0; i < col.length; i++) {
                var th = document.createElement("th");
                th.innerHTML = col[i];
                tr.appendChild(th);
            }

            for (i = 0; i < receipts.length; i++) {
                tr = table.insertRow(-1);
                for (var j = 0; j < col.length; j++) {
                    var tabCell = tr.insertCell(-1);
                    tabCell.innerHTML = receipts[i][col[j]];
                }
            }
            document.getElementById('receipts').style.width = '95%';
            document.getElementById('receipts').style.height = '95%';
            table.style.display = 'inline-block';
            document.getElementById('receipts').appendChild(table);
        }
    }
    xhr.send(data);
}
