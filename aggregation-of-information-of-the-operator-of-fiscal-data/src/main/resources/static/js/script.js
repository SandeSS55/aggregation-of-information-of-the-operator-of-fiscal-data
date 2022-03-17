let thisInn;
let showFlag=false;
function showKKTs(kktset,inn) {
    document.getElementById('main').innerHTML = '';
	document.getElementById('header').innerHTML='';
    let check = kktset;
    thisInn=inn;
    for (let i = check.length - 1; i >= 0; i--) {
        document.getElementById('main').innerHTML += "<div><p>Идентификационный номер в SQL: " + check[i].id + "</p><p>Регистрационный номер кассы: " +
            check[i].kktRegNumber + ";</p><p>Адрес: " + check[i].fiscalAddress + "</p>" +
            "<input type=\"checkbox\" id=\"" + check[i].id + "\" name=kkt value=\"" + check[i].kktRegNumber + "\"></div>";
    }
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
		
	document.getElementById('header').innerHTML +="<button class='button update' id='btnUpdate' name=post  onclick=document.getElementById('blurBackground').style.display='flex';" +
        "document.getElementById('update').style.display='flex';>Обновить текущую базу данных</button>";

	if(showFlag===false){
		document.getElementById('blurBackground').querySelectorAll('button[name="post"]').forEach(function (item) {
        item.addEventListener('click', function () {
            document.getElementById('blurBackground').querySelectorAll('.lds-dual-ring').forEach(el => el.style.display = 'none');
            document.getElementById('blurBackground').querySelectorAll("button[name='report']").forEach(el => el.style.display = 'flex');
            document.getElementById('blurBackground').querySelectorAll('p[id="response"]').forEach(el => el.style.display = 'none');
        })
    });

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
                        createPeriodReport(from, to,values);
                    }
                    break
                case 'deleteInnBtn':
                    deleteInn(thisInn);
                    break
            }
        });
    })
	showFlag=true;
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

function updateButton(){
	updateBase();
}

function closeBtn(name, loading, btn) {
    document.getElementById(name).style.display = 'none';
    document.getElementById(btn).style.display = 'flex';
    document.getElementById('blurBackground').style.display = 'none';
    document.getElementById(loading).style.display = 'none';
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
    var from = moment(to).subtract(1, 'days').format();

    var xhr = new XMLHttpRequest();
    var url = "/shifts/reports";
    xhr.open("POST", url, true);
    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.onreadystatechange = function () {
        if (xhr.readyState === 4 && xhr.status === 200) {
            closeBtn2('day');
            document.getElementById('dayLoading').style.display = 'none';
            let p = document.createElement('p');
            p.id = 'response';
            document.getElementById('day').appendChild(p);
            document.getElementById('response').innerHTML += xhr.responseText;
        }
    };
    var data = JSON.stringify({
        "from": from.toString().substr(0, from.toString().length - 6),
        "to": to.toString().substr(0, to.toString().length - 6), "kkts": values
    });
    xhr.send(data);
}

function createWeekReport(values) {
    document.getElementById('weekBtn').style.display = 'none';
    document.getElementById('weekLoading').style.display = 'flex';

    var to = moment().format();  
    var from = moment(to).subtract(1, 'weeks').format();
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
            document.getElementById('response').innerHTML += xhr.responseText;
        }
    };
    var data = JSON.stringify({
        "from": from.toString().substr(0, from.toString().length - 6), 
        "to": to.toString().substr(0, to.toString().length - 6), "kkts": values
    });
    xhr.send(data); //отправляем
}


function createMonthReport(values) {
    document.getElementById('monthBtn').style.display = 'none';
    document.getElementById('monthLoading').style.display = 'flex';

    var to = moment().format();
    var from = moment(to).subtract(1, 'months').format();

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
            document.getElementById('response').innerHTML += xhr.responseText;
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
    var from = moment(to).subtract(1, 'years').format();
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
            document.getElementById('response').innerHTML += xhr.responseText;
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
            /*closeBtn2('deleteInn');*/
            document.getElementById('deleteInnLoading').style.display = 'none';
            let p = document.createElement('p');
            p.id = 'response';
            document.getElementById('deleteInn').appendChild(p);
            document.getElementById('response').innerHTML += xhr.responseText;
			document.getElementById('deleteInn').querySelector('button[name="close"]').onclick=function(){
            location.href='main';
            };
        }
    };
    var data = JSON.stringify({
        "inn":inn
    });
    xhr.send(data);
}

function createPeriodReport(from,to,values) {
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
                document.getElementById('response').innerHTML += xhr.responseText;
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
			document.getElementById('addInn').querySelector('button[name="close"]').onclick=function(){
            location.href='main';
            };
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
            if (xhr.readyState===4 && xhr.status !== 200) {
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

function updateBase(){
	document.getElementById('updateBtn').style.display = 'none';
    document.getElementById('updateLoading').style.display = 'flex';
	var xhr = new XMLHttpRequest();
    var url = "/shifts/update";
    xhr.open("GET", url, true);
    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.onreadystatechange = function () {
		if(xhr.readyState===4 && xhr.status===200){
            document.getElementById('updateLoading').style.display = 'none';
            let p = document.createElement('p');
            p.id = 'response';
            document.getElementById('update').appendChild(p);
            document.getElementById('response').innerHTML += xhr.responseText;
			document.getElementById('update').querySelector('button[name="close"]').onclick=function(){
			location.href='main';
			};
		}
		if(xhr.readyState===4 && xhr.status!==200){
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
