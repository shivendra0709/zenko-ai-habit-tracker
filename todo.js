// Todo Dashboard App
class TodoApp {
    constructor() {
        this.todos = JSON.parse(localStorage.getItem('todos')) || [];
        this.currentFilter = 'all';
        this.isDarkMode = localStorage.getItem('darkMode') === 'true';

        // Cache DOM elements
        this.elements = {
            currentDate: document.getElementById('currentDate'),
            currentDay: document.getElementById('currentDay'),
            themeToggle: document.getElementById('themeToggle'),
            totalTasks: document.getElementById('totalTasks'),
            activeTasks: document.getElementById('activeTasks'),
            completedTasks: document.getElementById('completedTasks'),
            progressPercentage: document.getElementById('progressPercentage'),
            progressBar: document.getElementById('progressBar'),
            progressText: document.getElementById('progressText'),
            taskInput: document.getElementById('taskInput'),
            addBtn: document.getElementById('addBtn'),
            todoList: document.getElementById('todoList'),
            emptyState: document.getElementById('emptyState'),
            allCount: document.getElementById('allCount'),
            activeCount: document.getElementById('activeCount'),
            completedCount: document.getElementById('completedCount'),
            clearCompletedBtn: document.getElementById('clearCompleted'),
            tabs: document.querySelectorAll('.tab')
        };

        // Set initial theme
        if (this.isDarkMode) {
            document.body.classList.add('dark-mode');
        }

        this.init();
    }

    init() {
        this.setDateTime();
        this.initEventListeners();
        this.render();
    }

    setDateTime() {
        const now = new Date();
        const options = { month: 'short', day: 'numeric' };
        this.elements.currentDate.textContent = now.toLocaleDateString('en-US', options);
        this.elements.currentDay.textContent = now.toLocaleDateString('en-US', { weekday: 'long' });
    }

    initEventListeners() {
        // Add todo
        this.elements.addBtn.addEventListener('click', () => this.addTodo());
        this.elements.taskInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') this.addTodo();
        });

        // Clear completed
        this.elements.clearCompletedBtn.addEventListener('click', () => this.clearCompleted());

        // Filter tabs
        this.elements.tabs.forEach(tab => {
            tab.addEventListener('click', (e) => {
                const filter = e.currentTarget.dataset.filter;
                this.setFilter(filter);
            });
        });

        // Theme toggle
        this.elements.themeToggle.addEventListener('click', () => this.toggleTheme());

        // Todo interactions (delegation)
        this.elements.todoList.addEventListener('click', (e) => {
            const item = e.target.closest('.todo-item');
            if (!item) return;

            const id = parseInt(item.dataset.id);

            if (e.target.classList.contains('todo-checkbox')) {
                this.toggleTodo(id);
            } else if (e.target.closest('.delete-btn')) {
                this.deleteTodo(id);
            }
        });

        // Touch gestures (optional: swipe to delete)
        this.setupSwipeGestures();
    }

    setupSwipeGestures() {
        let touchStartX = 0;
        let touchEndX = 0;

        this.elements.todoList.addEventListener('touchstart', (e) => {
            touchStartX = e.changedTouches[0].screenX;
        }, { passive: true });

        this.elements.todoList.addEventListener('touchend', (e) => {
            touchEndX = e.changedTouches[0].screenX;
            this.handleSwipe(touchStartX, touchEndX);
        }, { passive: true });
    }

    handleSwipe(startX, endX) {
        const threshold = 50;
        const diff = startX - endX;

        if (Math.abs(diff) > threshold) {
            const item = document.elementFromPoint(startX, document.querySelector('.todo-item')?.getBoundingClientRect().top || 0);
            const todoItem = item?.closest('.todo-item');
            if (todoItem && diff > 0) {
                const id = parseInt(todoItem.dataset.id);
                this.deleteTodo(id);
            }
        }
    }

    addTodo() {
        const text = this.elements.taskInput.value.trim();

        if (text === '') {
            this.shakeInput();
            return;
        }

        const newTodo = {
            id: Date.now(),
            text: text,
            completed: false,
            createdAt: new Date().toISOString()
        };

        this.todos.unshift(newTodo);
        this.saveTodos();
        this.elements.taskInput.value = '';
        this.elements.taskInput.focus();
        this.render();
    }

    shakeInput() {
        const input = this.elements.taskInput;
        input.style.animation = 'shake 0.4s ease';
        setTimeout(() => input.style.animation = '', 400);
    }

    toggleTodo(id) {
        const todo = this.todos.find(t => t.id === id);
        if (todo) {
            todo.completed = !todo.completed;
            this.saveTodos();
            this.render();
        }
    }

    deleteTodo(id) {
        const item = document.querySelector(`[data-id="${id}"]`);
        if (item) {
            item.style.animation = 'slideOut 0.3s ease forwards';
            setTimeout(() => {
                this.todos = this.todos.filter(t => t.id !== id);
                this.saveTodos();
                this.render();
            }, 300);
        } else {
            this.todos = this.todos.filter(t => t.id !== id);
            this.saveTodos();
            this.render();
        }
    }

    clearCompleted() {
        this.todos = this.todos.filter(t => !t.completed);
        this.saveTodos();
        this.render();
    }

    setFilter(filter) {
        this.currentFilter = filter;

        this.elements.tabs.forEach(tab => {
            tab.classList.remove('active');
            if (tab.dataset.filter === filter) {
                tab.classList.add('active');
            }
        });

        this.render();
    }

    toggleTheme() {
        this.isDarkMode = !this.isDarkMode;
        document.body.classList.toggle('dark-mode');
        localStorage.setItem('darkMode', this.isDarkMode);
    }

    getStats() {
        const total = this.todos.length;
        const completed = this.todos.filter(t => t.completed).length;
        const active = total - completed;
        const percentage = total > 0 ? Math.round((completed / total) * 100) : 0;

        return { total, completed, active, percentage };
    }

    getFilteredTodos() {
        switch (this.currentFilter) {
            case 'active':
                return this.todos.filter(t => !t.completed);
            case 'completed':
                return this.todos.filter(t => t.completed);
            default:
                return this.todos;
        }
    }

    saveTodos() {
        localStorage.setItem('todos', JSON.stringify(this.todos));
    }

    render() {
        const stats = this.getStats();
        const filteredTodos = this.getFilteredTodos();

        // Update stats cards
        this.animateNumber(this.elements.totalTasks, stats.total);
        this.animateNumber(this.elements.activeTasks, stats.active);
        this.animateNumber(this.elements.completedTasks, stats.completed);

        // Update progress bar
        this.elements.progressPercentage.textContent = `${stats.percentage}%`;
        this.elements.progressBar.style.width = `${stats.percentage}%`;

        if (stats.total === 0) {
            this.elements.progressText.textContent = 'Add tasks to start';
            this.elements.progressBar.style.background = 'rgba(0,0,0,0.1)';
        } else if (stats.percentage === 100) {
            this.elements.progressText.textContent = '🎉 All tasks completed!';
            this.elements.progressBar.style.background = 'linear-gradient(135deg, #10b981 0%, #34d399 100%)';
        } else {
            this.elements.progressText.textContent = `${stats.active} task${stats.active !== 1 ? 's' : ''} remaining`;
        }

        // Update tab counts
        this.elements.allCount.textContent = stats.total;
        this.elements.activeCount.textContent = stats.active;
        this.elements.completedCount.textContent = stats.completed;

        // Show/hide empty state
        if (filteredTodos.length === 0) {
            this.elements.emptyState.classList.remove('hidden');
            this.elements.todoList.style.display = 'none';
        } else {
            this.elements.emptyState.classList.add('hidden');
            this.elements.todoList.style.display = 'flex';
        }

        // Render todo items
        this.elements.todoList.innerHTML = '';
        filteredTodos.forEach(todo => {
            const li = document.createElement('li');
            li.className = `todo-item ${todo.completed ? 'completed' : ''}`;
            li.dataset.id = todo.id;

            li.innerHTML = `
                <input type="checkbox" class="todo-checkbox" ${todo.completed ? 'checked' : ''}>
                <span class="task-text">${this.escapeHtml(todo.text)}</span>
                <button class="delete-btn">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <line x1="18" y1="6" x2="6" y2="18"></line>
                        <line x1="6" y1="6" x2="18" y2="18"></line>
                    </svg>
                </button>
            `;

            this.elements.todoList.appendChild(li);
        });
    }

    animateNumber(element, target) {
        const current = parseInt(element.textContent) || 0;
        if (current === target) return;

        const duration = 300;
        const steps = 20;
        const increment = (target - current) / steps;
        let step = 0;

        const timer = setInterval(() => {
            step++;
            const value = Math.round(current + increment * step);
            element.textContent = value;

            if (step >= steps) {
                element.textContent = target;
                clearInterval(timer);
            }
        }, duration / steps);
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}

// Add shake animation
const style = document.createElement('style');
style.textContent = `
    @keyframes shake {
        0%, 100% { transform: translateX(0); }
        25% { transform: translateX(-10px); }
        75% { transform: translateX(10px); }
    }
    @keyframes slideOut {
        to {
            opacity: 0;
            transform: translateX(100%);
        }
    }
    .dark-mode {
        --glass-bg: rgba(30, 30, 40, 0.4);
        --glass-border: rgba(255, 255, 255, 0.1);
        --text-primary: #ffffff;
        --text-secondary: rgba(255, 255, 255, 0.6);
    }
    .dark-mode body::before {
        background:
            radial-gradient(circle at 30% 30%, rgba(102, 126, 234, 0.2) 0%, transparent 50%),
            radial-gradient(circle at 70% 70%, rgba(118, 75, 162, 0.2) 0%, transparent 50%);
    }
`;
document.head.appendChild(style);

// Initialize the app when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    new TodoApp();
});
